package net.engineerAnsh.BankApplication.Kafka.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.UserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRegisteredEventProducer{

    private static final String Topic = "user-registration-topic";

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public void publishUserRegistrationEventSuccess(UserRegisteredEvent event){
        // Here, Key: user email.
        // Value: event object (auto-converted to JSON).
        kafkaTemplate.send(Topic,event.getEmail(),event);
        log.info("Verification token event published for user: {}", event.getEmail());
    }
}
