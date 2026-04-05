package net.engineerAnsh.BankApplication.Kafka.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Dto.FailedKafkaEventResponse;
import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionEvent;
import net.engineerAnsh.BankApplication.Kafka.Producer.TransactionEventProducer;
import net.engineerAnsh.BankApplication.Kafka.Repository.FailedKafkaEventRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FailedKafkaEventService {

    private final FailedKafkaEventRepository failedKafkaEventRepository;
    private final TransactionEventProducer transactionEventProducer;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private boolean isMaxRetryExceeded(FailedKafkaEvent event) {
        if (event.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            log.warn("Max retry attempts reached, marking the {} event as PERMANENTLY_FAILED ", event.getEventType());
            event.setStatus(FailedEventStatus.PERMANENTLY_FAILED);
            saveFailedKafkaEvent(event);
            log.info("event: {} is successfully marked as PERMANENTLY_FAILED", event.getEventType());
            return true;
        }
        return false;
    }

    private FailedKafkaEventResponse mapToFailedKafkaEventResponse(FailedKafkaEvent event) {
        return new FailedKafkaEventResponse(
                event.getEventId(),
                event.getTopic(),
                event.getEventType(),
                event.getStatus(),
                event.getErrorMessage(),
                event.getRetryCount()
        );
    }

    public List<FailedKafkaEventResponse> findPendingFailedKafkaEvents() {
        return failedKafkaEventRepository.findByStatusNotIn(
                List.of(FailedEventStatus.RESOLVED, FailedEventStatus.PERMANENTLY_FAILED)
        ).stream().map(this::mapToFailedKafkaEventResponse).toList();
    }

    public List<FailedKafkaEvent> findAllFailedEvents() {
        return failedKafkaEventRepository
                .findByStatusNotIn(
                        List.of(FailedEventStatus.RESOLVED, FailedEventStatus.PERMANENTLY_FAILED)
                );
    }

    public FailedKafkaEvent findFailedKafkaEventByEventId(String eventId) {
        return failedKafkaEventRepository
                .findByEventId(eventId).orElse(null);
    }

    public void saveFailedKafkaEvent(FailedKafkaEvent event) {
        failedKafkaEventRepository.save(event);
    }

    private void retryFailedEventInternally(FailedKafkaEvent failedEvent) throws Exception {

        if (isMaxRetryExceeded(failedEvent)) return;

        try {

            // Convert the payload back to the 'transactionEvent'...
            TransactionEvent event = objectMapper.readValue(
                    failedEvent.getPayload(),
                    TransactionEvent.class
            );

            // re-publish the event to kafka...
            transactionEventProducer.publishTxnEvent(event);

            // Update status...
            failedEvent.setStatus(FailedEventStatus.RETRIED); // we retried the failedEvent...
            failedEvent.setRetryCount(failedEvent.getRetryCount() + 1);
            failedEvent.setLastRetriedAt(LocalDateTime.now());
            saveFailedKafkaEvent(failedEvent);

            log.info("Successfully retried event: {}", failedEvent.getEventType());

        } catch (Exception e) {
            log.error("Retry failed for event: {}", failedEvent.getEventType(), e);
            throw e;
        }
    }

    public void retryFailedEventByEventId(String eventId) throws Exception {
        FailedKafkaEvent failedKafkaEvent = failedKafkaEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new EntityNotFoundException("No failed kafka event is found..."));
        retryFailedEventInternally(failedKafkaEvent);
    }

    public void retryFailedEventById(Long id) throws Exception {
        FailedKafkaEvent failedEvent = failedKafkaEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Failed event not found"));
        retryFailedEventInternally(failedEvent);
    }

    // Retry all failed events...
    public void retryAllFailedEvents() {
        List<FailedKafkaEvent> failedKafkaEvents = findAllFailedEvents();

        for (FailedKafkaEvent failedEvent : failedKafkaEvents) {

            if (isMaxRetryExceeded(failedEvent)) continue;

            try {

                // Convert the payload back to the 'transactionEvent'...
                TransactionEvent event = objectMapper.readValue(
                        failedEvent.getPayload(),
                        TransactionEvent.class
                );

                // re-publish the event to kafka...
                transactionEventProducer.publishTxnEvent(event);

                // Update status...
                failedEvent.setStatus(FailedEventStatus.RETRIED);
                failedEvent.setRetryCount(failedEvent.getRetryCount() + 1);
                failedEvent.setLastRetriedAt(LocalDateTime.now());

            } catch (Exception e) {
                log.error("Retry failed for event: {}", failedEvent.getEventType(), e);
            }
        }
        failedKafkaEventRepository.saveAll(failedKafkaEvents);
    }
}
