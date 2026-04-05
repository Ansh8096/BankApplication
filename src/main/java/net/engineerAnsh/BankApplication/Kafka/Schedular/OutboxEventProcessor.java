package net.engineerAnsh.BankApplication.Kafka.Schedular;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Kafka.Service.OutboxSingleEventProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {

    private final OutboxSingleEventProcessor outboxSingleEventProcessor;

    @Scheduled(fixedDelayString = "${outbox.poll.interval}")
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxSingleEventProcessor.fetchEventsForProcessing();

        if (events.isEmpty()) {
            log.info("No outbox events to process...");
            return;
        }

        for (OutboxEvent event : events) {
            outboxSingleEventProcessor.processSingleEvent(event); // each event handled separately...
        }
    }
}

