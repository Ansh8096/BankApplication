package net.engineerAnsh.BankApplication.Schedular;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Enum.outbox.OutboxStatus;
import net.engineerAnsh.BankApplication.Repository.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupJob {

    private final OutboxEventRepository repository;

    @Scheduled(cron = "0 0 * * * *") // every hour...
    @Transactional
    public void cleanupProcessedEvents() {
        LocalDateTime cutoff =
                LocalDateTime.now().minusHours(24);

        int deleted = repository.deleteProcessedEvents(
                OutboxStatus.PROCESSED,
                cutoff
        );

        if (deleted > 0) {
            log.info("Outbox cleanup removed {} processed events", deleted);
        }
    }
}
