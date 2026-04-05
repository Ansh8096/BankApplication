package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.UserRegisteredEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final EmailServiceImpl emailService;

    @KafkaListener(
            topics = "user-events",
            groupId = "email-service-group"
    )
    public void handleUserEvents(ConsumerRecord<String,Object> consumerRecord) {

        Object event = consumerRecord.value(); // this is the actual event...
        try {
            if (event instanceof UserRegisteredEvent regEvent) {

                log.info("Processing USER_REGISTERED for: {}", regEvent.getEmail());

                emailService.sendVerificationEmail(
                        regEvent.getEmail(),
                        regEvent.getVerificationToken()
                );

            } else if (event instanceof UserLoginEvent loginEvent) {

                log.info("Processing USER_LOGIN for: {}", loginEvent.getEmail());

                emailService.sendLoginAlertEmail(loginEvent);

            } else {
                log.warn("Unknown event type received: {}", event.getClass());
            }

        } catch (Exception e) {
            log.error("Error processing user event", e);
        }
    }
}