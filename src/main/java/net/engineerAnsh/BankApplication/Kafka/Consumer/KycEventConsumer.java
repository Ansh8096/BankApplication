package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.services.notification.email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.services.notification.email.EmailTemplateService;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.kyc.KycStatus;
import net.engineerAnsh.BankApplication.Kafka.Enums.KycEventType;
import net.engineerAnsh.BankApplication.Kafka.Event.KycEvent;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycEventConsumer {

    private final EmailServiceImpl emailService;
    private final UserRepository userRepository;
    private final EmailTemplateService emailTemplateService;

    private void notifyAdmins(KycEvent event) {
        String subject = "New KYC Submission - Admin Alert";
        String body = emailTemplateService.buildKycAlertEmailForAdmin(event);

        List<User> admins = userRepository.findByRoles_NameAndKycStatus("ROLE_ADMIN", KycStatus.APPROVED);
        log.info("Total admins available in system are: {}", admins.size());

        for (User admin : admins) {
            emailService.sendHtmlEmail(admin.getEmail(),
                    subject,
                    body
            );
            log.info("Admin: {} has successfully received a new kyc alert email", admin.getEmail());
        }
    }

    private String buildSubject(KycEvent event) {
        return switch (event.getEventType()) {
            case SUBMITTED -> "KYC Submitted";
            case APPROVED -> "KYC Approved";
            case REJECTED -> "KYC Rejected";
        };
    }

    @KafkaListener(topics = "kyc-events",
            groupId = "kyc-notification-group"
    )
    public void consumeKycEvent(KycEvent event) {

        log.info("Received KYC event: {}", event);
        String subject = buildSubject(event);
        String body = emailTemplateService.buildKycAlertEmail(event);
        // Notifying the admins, whenever a new kyc is available to review...
        if (event.getEventType() == KycEventType.SUBMITTED) notifyAdmins(event);

        emailService.sendHtmlEmail(event.getEmail(), subject, body);
    }

}
