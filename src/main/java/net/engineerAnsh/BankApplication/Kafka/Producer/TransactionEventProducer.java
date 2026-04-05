package net.engineerAnsh.BankApplication.Kafka.Producer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventProducer {

    private static final String TOPIC = "transaction-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Generic eventType extractor (no duplication)...
    private String getEventType(TransactionEvent event) {
        JsonTypeName annotation = event.getClass().getAnnotation(JsonTypeName.class);
        return annotation != null ? annotation.value() : "UNKNOWN";
    }

    private String extractEventId(TransactionEvent event) {
        return ((IdentifiableEvent) event).getEventId();
    }

    public void publishTxnEvent(TransactionEvent event) {

        // Extract eventType dynamically from annotation...
        String eventType = getEventType(event);

        // Using eventType and eventId as our key...
        String key = eventType + "_" + extractEventId(event);

        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish an Transaction event: {} ", eventType, ex);
                    } else {
                        log.info("Successfully published a transaction event: {}", eventType);
                    }
                });
    }

}
