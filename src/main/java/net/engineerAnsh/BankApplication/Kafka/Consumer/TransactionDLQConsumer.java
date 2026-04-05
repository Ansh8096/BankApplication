package net.engineerAnsh.BankApplication.Kafka.Consumer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Builder.FailedKafkaEventBuilder;
import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.IdentifiableEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionEvent;
import net.engineerAnsh.BankApplication.Kafka.Service.FailedKafkaEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDLQConsumer {

    private final FailedKafkaEventService failedKafkaEventService;
    private final FailedKafkaEventBuilder failedKafkaEventBuilder;
    private final ObjectMapper objectMapper;

    private String getEventType(TransactionEvent event) {
        JsonTypeName annotation = event.getClass().getAnnotation(JsonTypeName.class);
        return annotation != null ? annotation.value() : "UNKNOWN";
    }

    private String extractEventId(TransactionEvent event) {
        return ((IdentifiableEvent) event).getEventId();
    }


    @KafkaListener(
            topics = "transaction-events.dlt",
            groupId = "transaction-dlq-group"
    )
    public void consumeDLQ(TransactionEvent event) {

        // extracting the event type...
        String eventType = getEventType(event);

        log.error("💀 DLQ MESSAGE RECEIVED: {}", eventType);

        try {
            // Check if already stored...
            FailedKafkaEvent existing = failedKafkaEventService
                    .findFailedKafkaEventByEventId(extractEventId(event));

            if (existing != null) {
                log.warn("Event already stored, skipping: {}", eventType);
                return;
            }

            // Convert event to JSON...
            String payload = objectMapper.writeValueAsString(event);

            // Build failed event...
            FailedKafkaEvent failedEvent = failedKafkaEventBuilder.buildFailedKafkaEvent(
                    extractEventId(event),
                    "transaction-events",
                    "Moved to DLQ after retries exhausted",
                    payload,
                    eventType
            );

            // Save to DB...
            failedKafkaEventService.saveFailedKafkaEvent(failedEvent);

            log.error("Failed event stored in DB for: {}", eventType);

        } catch (Exception e) {
            log.error("Failed to process DLQ event", e);
        }
    }
}
