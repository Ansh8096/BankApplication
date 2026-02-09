package net.engineerAnsh.BankApplication.Kafka.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Kafka.Producer.TransactionEventProducer;
import net.engineerAnsh.BankApplication.Kafka.Repository.FailedKafkaEventRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FailedKafkaEventService {

    private final FailedKafkaEventRepository failedKafkaEventRepository;
    private final TransactionEventProducer transactionEventProducer;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private boolean checkRetryCount(FailedKafkaEvent event){
        if (event.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            log.warn("Max retry attempts reached for event {}", event.getTransactionReference());
            return false;
        }
        return true;
    }

    // Retry a single failed event...
    public void retryFailedEvent(Long id){
        FailedKafkaEvent failedEvent = failedKafkaEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Failed event not found"));

        if(!checkRetryCount(failedEvent)) return;

        try {
            // Convert the payload back to the 'transactionCompletedEvent'...
            TransactionCompletedEvent event = objectMapper
                    .readValue(failedEvent.getPayload(), TransactionCompletedEvent.class);


            // re-publish the event to kafka...
            transactionEventProducer.publishTransactionCompleted(event);

            // Update status...
            failedEvent.setStatus(FailedEventStatus.RETRIED); // we retried the failedEvent...
            failedEvent.setRetryCount(failedEvent.getRetryCount() + 1);
            failedKafkaEventRepository.save(failedEvent);

            log.info("Successfully retried event: {}", failedEvent.getTransactionReference());

        } catch (JsonProcessingException e) {
            log.error("Retry failed for event: {}", failedEvent.getTransactionReference(), e);
            throw new RuntimeException("Retry Failed...");
        }
    }

    // Retry all failed events...
    public void retryAllFailedEvents() throws JsonProcessingException {
        List<FailedKafkaEvent> failedKafkaEvents = failedKafkaEventRepository
                .findByStatusNot(FailedEventStatus.RESOLVED);

        for (FailedKafkaEvent failedEvent: failedKafkaEvents){

            if(!checkRetryCount(failedEvent)) continue;

            try {
                TransactionCompletedEvent event = objectMapper
                        .readValue(failedEvent.getPayload(), TransactionCompletedEvent.class);

                // re-publish the event to kafka...
                transactionEventProducer.publishTransactionCompleted(event);

                // Update status...
                failedEvent.setStatus(FailedEventStatus.RETRIED);
                failedEvent.setRetryCount(failedEvent.getRetryCount() + 1);

            } catch (JsonProcessingException e) {
                log.error("Retry failed for event: {}", failedEvent.getTransactionReference(), e);
                throw e;
            }
        }
        failedKafkaEventRepository.saveAll(failedKafkaEvents);
    }
}
