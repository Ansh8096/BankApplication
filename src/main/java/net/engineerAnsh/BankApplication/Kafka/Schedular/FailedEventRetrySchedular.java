package net.engineerAnsh.BankApplication.Kafka.Schedular;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Service.FailedKafkaEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailedEventRetrySchedular {

    private final FailedKafkaEventService failedKafkaEventService;

    // Runs every 5 minutes
    @Scheduled(fixedRateString = "${app.kafka.retry-interval-ms}")
    public void retryFailedEvents() {

        List<FailedKafkaEvent> failedEvents = failedKafkaEventService.findAllFailedEvents();

        if (failedEvents.isEmpty()) {
            log.info("No failed KAFKA events are available...");
            return;
        }

        log.info("Retry scheduler found {} failed events to process.", failedEvents.size());

        for (FailedKafkaEvent event : failedEvents) {
            try {
                failedKafkaEventService.retryFailedEventById(event.getId());
            } catch (Exception e) {
                log.error("Scheduler retry failed for event {}",
                        event.getEventType());
            }
        }
    }

}
