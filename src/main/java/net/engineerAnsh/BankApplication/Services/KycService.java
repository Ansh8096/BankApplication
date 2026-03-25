package net.engineerAnsh.BankApplication.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycReviewRequest;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycSubmissionRequest;
import net.engineerAnsh.BankApplication.Entity.KycVerification;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.KycStatus;
import net.engineerAnsh.BankApplication.Enum.OutboxEventType;
import net.engineerAnsh.BankApplication.Kafka.Enums.KycEventType;
import net.engineerAnsh.BankApplication.Kafka.Builder.KycEventBuilder;
import net.engineerAnsh.BankApplication.Kafka.Event.KycEvent;
import net.engineerAnsh.BankApplication.Repository.KycVerificationRepository;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycService {

    private final KycVerificationRepository kycRepository;
    private final UserRepository userRepository;
    private final OutboxEventService outboxEventService;
    private final KycEventBuilder kycEventBuilder;

    private void buildAndSaveOutboxEvent(KycEvent event) throws JsonProcessingException {
        OutboxEvent outboxKycEvent = outboxEventService.buildOutboxEvent(event, OutboxEventType.KYC_EVENT);
        outboxEventService.publishOutBoxEvent(outboxKycEvent);
    }

    private void setKycRecord(KycVerification kyc, KycSubmissionRequest request){
        kyc.setDocumentType(request.getDocumentType().name());
        kyc.setDocumentNumber(request.getDocumentNumber());
        kyc.setDocumentUrl(request.getDocumentUrl());
        kyc.setStatus(KycStatus.SUBMITTED);
        kyc.setSubmittedAt(LocalDateTime.now());
        kyc.setRejectionReason(null);
        kyc.setReviewedAt(null);
        kyc.setReviewedBy(null);
    }

    @Transactional
    public void submitKyc(Long userId, KycSubmissionRequest request) throws JsonProcessingException {

        User user = userRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new EntityNotFoundException("No Active user found"));

        Optional<KycVerification> existingKyc = kycRepository.findByUserUserId(userId);

        // It means if the Kyc is rejected, then users are allowed to resubmit the documents...
        if (existingKyc.isPresent() && existingKyc.get().getStatus() != KycStatus.REJECTED) {

            throw new EntityExistsException("KYC is already submitted and under review...");
        }

        KycVerification kyc;

        if (existingKyc.isPresent()) {
            // reuse existing record...
            kyc = existingKyc.get();
        } else {
            kyc = new KycVerification();
            kyc.setUser(user);
        }

        // Sets details of kyc record
        setKycRecord(kyc, request);

        KycEvent kycSubmitEvent = kycEventBuilder
                .buildKycEvent(user, KycEventType.SUBMITTED, null, request.getDocumentType().name());

        user.setKycStatus(KycStatus.UNDER_REVIEW);
        userRepository.save(user);

        buildAndSaveOutboxEvent(kycSubmitEvent);
        kycRepository.save(kyc);
    }

    public KycVerification getUserKyc(Long userId) {
        return kycRepository.findByUserUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC not submitted"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<KycVerification> getPendingKyc() {
        return kycRepository.findByStatus(KycStatus.SUBMITTED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void reviewKyc(Long kycId, String adminEmail, KycReviewRequest request) throws JsonProcessingException {

        KycVerification kyc = kycRepository.findById(kycId)
                .orElseThrow(() -> new EntityNotFoundException("KYC not found"));

        if(kyc.getStatus() != KycStatus.SUBMITTED){
            throw new IllegalStateException("This Kyc documents are already reviewed...");
        }

        User user = kyc.getUser();

        if (Boolean.FALSE.equals(request.getApproved())) {

            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new IllegalStateException("Rejection reason required");
            }

            kyc.setStatus(KycStatus.REJECTED);
            kyc.setRejectionReason(request.getRejectionReason());
            user.setKycStatus(KycStatus.REJECTED);

            KycEvent kycRejectedEvent = kycEventBuilder.buildKycEvent(
                    user, KycEventType.REJECTED, request.getRejectionReason(), null);

            buildAndSaveOutboxEvent(kycRejectedEvent);

        } else {

            kyc.setStatus(KycStatus.APPROVED);
            user.setKycStatus(KycStatus.APPROVED);

            KycEvent kycApprovedEvent = kycEventBuilder
                    .buildKycEvent(user, KycEventType.APPROVED, null, null);

            buildAndSaveOutboxEvent(kycApprovedEvent);
        }

        kyc.setReviewedAt(LocalDateTime.now());
        kyc.setReviewedBy(adminEmail);
        userRepository.save(user);
        kycRepository.save(kyc);
    }
}
