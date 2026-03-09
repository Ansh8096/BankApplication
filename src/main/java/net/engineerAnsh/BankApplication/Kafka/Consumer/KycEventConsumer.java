package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Entity.User;
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

    private void notifyAdmins(KycEvent event) {

        String subject = "New KYC Submission";
        String message = """
                A new KYC verification request has been submitted.

                User ID: %s
                Email: %s
                Document Type: %s

                Please review it in the admin dashboard.
                """.formatted(
                event.getUserId(),
                event.getEmail(),
                event.getDocumentType()
        );

        List<User> admins = userRepository.findByRoles_Name("ROLE_ADMIN");
        log.info("Total admins available in system are: {}",admins.size());

        for(User admin : admins) {
            emailService.sendSimpleEmail(admin.getEmail(),
                    subject,
                    message);
        }
    }

    @KafkaListener(topics = "kyc-events",
            groupId = "kyc-notification-group"
    )
    public void consumeKycEvent(KycEvent event) {

        log.info("Received KYC event: {}", event);

        switch (event.getEventType()) {

            case SUBMITTED:
                emailService.sendSimpleEmail(
                        event.getEmail(),
                        "KYC Submitted",
                        "Your KYC documents have been submitted and are under review."
                );

                // Notify the admin, whenever a new Kyc is available to review...
                notifyAdmins(event);
                break;

            case APPROVED:

                emailService.sendSimpleEmail(
                        event.getEmail(),
                        "KYC Approved",
                        "Your KYC verification has been approved. You can now perform transactions."
                );
                break;

            case REJECTED:
                emailService.sendSimpleEmail(
                        event.getEmail(),
                        "KYC Rejected",
                        "Your KYC verification was rejected. Reason: " + event.getReason()
                );
                break;
        }
    }

}
