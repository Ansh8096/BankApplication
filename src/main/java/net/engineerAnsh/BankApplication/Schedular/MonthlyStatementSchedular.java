package net.engineerAnsh.BankApplication.Schedular;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Enum.account.AccountStatus;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Services.StatementJobService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MonthlyStatementSchedular {

    private final AccountRepository accountRepository;
    private final StatementJobService statementJobService;

    // Scheduling runs the job at 12:05 AM on the 1st day of every month...
    // Sending the account statement of previous month on every first day of current month...
    @Scheduled(cron = "0 5 0 1 * ?", zone = "Asia/Kolkata")
    public void generateMonthlyStatements() {

        log.info("Starting monthly statement generation job");

        // Previous month calculation
        LocalDate now = LocalDate.now();
        LocalDate previousMonth = now.minusMonths(1);

        int month = previousMonth.getMonthValue();
        int year = previousMonth.getYear();

        // Fetch all active accounts, Also we need to use this method to fetch users eagerly,
        // to prevent 'LazyInitializationException' in async thread...
        List<Account> activeAccounts = accountRepository.findActiveAccountsWithUser(AccountStatus.ACTIVE);
        log.info("Found {} active accounts", activeAccounts.size());

        for (Account account : activeAccounts) {
            try {
                statementJobService.processStatementForAccount(account, month, year);
            } catch (Exception e) {
                log.error("Failed processing account {}", account.getAccountNumber(), e);
            }
        }
        log.info("Monthly statement job completed");
    }

}
