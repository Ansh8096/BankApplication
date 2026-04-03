package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.StatementJob;
import net.engineerAnsh.BankApplication.Enum.statement.StatementJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StatementJobRepository extends JpaRepository<StatementJob,Long>{

    // Find job for a specific account and month
    Optional<StatementJob> findByAccountNumberAndMonthAndYear(
            String accountNumber,
            int month,
            int year
    );

    // Find all failed jobs
    // do something about permanently_failed ones.......
    List<StatementJob> findByStatusNot(StatementJobStatus status);

    // Find failed jobs with retry limit
    List<StatementJob> findByStatusNotAndRetryCountLessThan(
            StatementJobStatus status,
            int retryCount
    );

    // Using 'In' keyword ...
    List<StatementJob> findByStatusIn(List<StatementJobStatus> statuses);

}
