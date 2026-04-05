package net.engineerAnsh.BankApplication.Kafka.EventHandlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import net.engineerAnsh.BankApplication.Kafka.Event.EventProcessedEvent;
import net.engineerAnsh.BankApplication.Kafka.Repository.FailedKafkaEventRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessedHandler implements TransactionEventHandler<EventProcessedEvent> {

    private final FailedKafkaEventRepository failedKafkaEventRepository;

    @Override
    public void handle(EventProcessedEvent event) {
        log.info("Success ACK received for event: {}", event.getEventType());
        try {
            // After success, If this event was a failed event mark it as resolved event now in the DB...
            failedKafkaEventRepository.findByEventId(event.getEventId())
                    .ifPresent(failedEvent -> {
                        failedEvent.setStatus(FailedEventStatus.RESOLVED);
                        failedKafkaEventRepository.save(failedEvent);
                    });

            log.info("Event resolved: {} type: {}", event.getEventId(), event.getEventType());

        } catch (Exception e) {
            log.error("Failed to resolve the event", e);
            // not throwing error to avoid retries...
        }
    }
}
