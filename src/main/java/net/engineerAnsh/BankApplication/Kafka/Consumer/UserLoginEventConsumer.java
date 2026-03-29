package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLoginEventConsumer {

    private final EmailServiceImpl emailService;

    @KafkaListener(topics = "user-login-topic",
            groupId = "email-service-group"
    )
    public void handleUserLoginEvent(UserLoginEvent event) {
        log.info("The successfully login event received for user: {}", event.getEmail());
        try {
            emailService.sendLoginAlertEmail(event);
        } catch (Exception e) {
            log.error("Failed to send login email to: {}", event.getEmail());
        }
    }

}
