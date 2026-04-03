package net.engineerAnsh.BankApplication.Schedular;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.StatementJob;
import net.engineerAnsh.BankApplication.Enum.statement.StatementJobStatus;
import net.engineerAnsh.BankApplication.Repository.StatementJobRepository;
import net.engineerAnsh.BankApplication.Services.StatementJobService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StatementRetrySchedular {

    private final StatementJobRepository statementJobRepository;
    private final StatementJobService statementJobService;

    // Runs every day at 3:00 AM...
    @Scheduled(cron = "0 0 3 * * ?")
    public void retryFailedStatements() {

        log.info("Starting statement retry scheduler");

        List<StatementJob> failedJobs = statementJobRepository.findByStatusIn(
                List.of(StatementJobStatus.FAILED,
                        StatementJobStatus.RETRIED
                )
        );

        if (failedJobs.isEmpty()) {
            log.info("No failed statement jobs to retry");
            return;
        }

        log.info("Retrying {} failed statement jobs", failedJobs.size());

        for (StatementJob job : failedJobs) {
            try {
                statementJobService.retryFailedJob(job);
            } catch (Exception e) {
                log.error("Retry failed for account {}",
                        job.getAccountNumber(), e);
            }
        }

        log.info("Statement retry scheduler completed");
    }
}
