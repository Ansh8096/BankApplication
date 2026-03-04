package net.engineerAnsh.BankApplication.Kafka.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserLoginEventProducer {

    private static final String Topic = "user-login-topic";

    private final KafkaTemplate<String, UserLoginEvent> kafkaTemplate;

    public void publishUserLoginEventSuccess(UserLoginEvent event){
        // Here, Key: user email.
        // Value: event object (auto-converted to JSON).
        kafkaTemplate.send(Topic,event.getEmail(),event);
        log.info("A successfully login event published for user: {}", event.getEmail());
    }

}
