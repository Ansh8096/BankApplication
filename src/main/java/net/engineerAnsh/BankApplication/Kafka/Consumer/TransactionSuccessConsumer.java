package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionSuccessEvent;
import net.engineerAnsh.BankApplication.Kafka.Repository.FailedKafkaEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSuccessConsumer {

    private final FailedKafkaEventRepository failedKafkaEventRepository;


    @KafkaListener(
            topics = "transaction.completed.success",
            groupId = "transaction-success-group"
    )
    public void consumeDLQ(TransactionSuccessEvent event) {
        log.info("Success ACK received for txn: {}", event.getTransactionReference());
        try {

            // After success, If this event was a failed event mark it as resolved event now...
            failedKafkaEventRepository.findByTransactionReference(event.getTransactionReference())
                    .ifPresent(failedEvent -> {
                        failedEvent.setStatus(FailedEventStatus.RESOLVED);
                        failedKafkaEventRepository.save(failedEvent);
                        log.info("Event marked as RESOLVED: {}", event.getTransactionReference());
                    });

        } catch (Exception e) {
            log.error("Failed to serialize DLQ event", e);
        }
    }
}
