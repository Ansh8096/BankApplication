package net.engineerAnsh.BankApplication.Kafka.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudEventProducer {

    private static final String KYC_TOPIC = "fraud-events";

    private final KafkaTemplate<String, FraudDetectedEvent> kafkaTemplate;

    public void publishFraudEvent(FraudDetectedEvent fraudEvent) {

        kafkaTemplate.send(KYC_TOPIC, fraudEvent.getEventId(), fraudEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish an fraud event of account {} decision: {}",
                                fraudEvent.getAccountNumber(), fraudEvent.getDecision(), ex);
                    } else {
                        log.info("Fraud event is published successfully of account {} decision: {}",
                                fraudEvent.getAccountNumber(), fraudEvent.getDecision());
                    }
                });
    }

}
