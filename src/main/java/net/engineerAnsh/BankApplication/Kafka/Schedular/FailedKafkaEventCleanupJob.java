package net.engineerAnsh.BankApplication.Kafka.Schedular;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import net.engineerAnsh.BankApplication.Kafka.Repository.FailedKafkaEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailedKafkaEventCleanupJob {

    private final FailedKafkaEventRepository repository;

    @Scheduled(cron = "${app.kafka.cleanup.cron}") // every hour
    @Transactional
    public void cleanupTerminalEvents() {

        // keep last 24 hours for safety/audit
        LocalDateTime cutoff =
                LocalDateTime.now().minusHours(24);

        int deleted = repository.deleteTerminalEvents(
                List.of(
                        FailedEventStatus.RESOLVED,
                        FailedEventStatus.PERMANENTLY_FAILED
                ),
                cutoff
        );

        if (deleted > 0) {
            log.info("FailedKafkaEvent cleanup removed {} terminal events", deleted);
        }
    }
}