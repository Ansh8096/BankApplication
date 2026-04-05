package net.engineerAnsh.BankApplication.Kafka.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.UserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private static final String TOPIC = "user-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserLoginEvent(UserLoginEvent event) {
        kafkaTemplate.send(TOPIC, event.getEmail(), event);
        log.info("Published USER_LOGIN event for user: {}", event.getEmail());
    }

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        kafkaTemplate.send(TOPIC, event.getEmail(), event);
        log.info("Published USER_REGISTERED event for user: {}", event.getEmail());
    }

}