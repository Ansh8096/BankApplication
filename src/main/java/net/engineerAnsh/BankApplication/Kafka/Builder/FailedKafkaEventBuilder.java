package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import org.springframework.stereotype.Service;

@Service
public class FailedKafkaEventBuilder {

    public FailedKafkaEvent buildFailedKafkaEvent(String eventId, String topic, String message, String payload, String eventType){
        // Build failed event entity...
        return FailedKafkaEvent.builder()
                .eventId(eventId)
                .topic(topic)
                .payload(payload)
                .errorMessage(message)
                .status(FailedEventStatus.FAILED)
                .eventType(eventType)
                .build();
    }
}
