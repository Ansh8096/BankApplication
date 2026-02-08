package net.engineerAnsh.BankApplication.Kafka.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Kafka.Repository.FailedKafkaEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDLQConsumer {

    private final FailedKafkaEventRepository failedKafkaEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "transaction.completed.dlt",
            groupId = "transaction-dlq-group"
    )
    public void consumeDLQ(TransactionCompletedEvent event) {
        log.error("💀 DLQ MESSAGE RECEIVED: {}", event.getTransactionReference());
        try {
            // Convert event to JSON...
            String payload = objectMapper.writeValueAsString(event);

            // Build failed event entity...
            FailedKafkaEvent failedEvent = FailedKafkaEvent.builder()
                    .transactionReference(event.getTransactionReference())
                    .topic("transaction.completed")
                    .payload(payload)
                    .errorMessage("Moved to DLQ after retries exhausted")
                    .status(FailedEventStatus.FAILED)
                    .build();

            // Save to DB...
            failedKafkaEventRepository.save(failedEvent);
            log.error("Failed event stored in DB for txn: {}",event.getTransactionReference());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DLQ event", e);
        }
    }
}
