package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Kafka.Event.UserRegisteredEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventConsumer {

    private final EmailServiceImpl emailService;

    @KafkaListener(topics = "user-registration-topic",
            groupId = "email-service-group"
    )
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("The verification token received for user: {}", event.getEmail());
        try {
            emailService.sendVerificationEmail(event.getEmail(),event.getVerificationToken());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", event.getEmail());
        }
    }
}
