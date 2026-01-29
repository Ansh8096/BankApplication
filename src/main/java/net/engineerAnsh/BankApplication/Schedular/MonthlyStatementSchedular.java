package net.engineerAnsh.BankApplication.Schedular;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Email.EmailServiceimpl;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Services.StatementPdfService;
import net.engineerAnsh.BankApplication.Services.TransactionService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MonthlyStatementSchedular {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final StatementPdfService statementPdfService;
    private final EmailServiceimpl emailService;

    private void sendMonthlyStatementsInternal(YearMonth month) {
        log.info("Monthly statement job started...");
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        log.info("Generating statements for period {} to {}", from, to);
        // We need to use this method to fetch users eagerly, to prevent 'LazyInitializationException' in async thread...
        List<Account> accounts = accountRepository.findActiveAccountsWithUser(AccountStatus.ACTIVE);

        for (Account account : accounts) {

            String accountNumber = account.getAccountNumber();
            String userEmail = account.getUser().getEmail(); // now we can get user (email & password) even when Hibernate session closes...
            String userName = account.getUser().getName();
            try {
                // generate statement dto...
                AccountStatementDto statementDto =
                        transactionService.generateMonthlyStatement(accountNumber, from, to);

                // generate pdf bytes...
                byte[] pdfBytes = statementPdfService.generatePdf(statementDto);

                String monthLabel = month.format(DateTimeFormatter.ofPattern("MMM-yyyy"));
                String subject = "Monthly Account Statement - " + monthLabel;

                String body = """
                        Hello %s,
                        
                        Please find attached your monthly account statement.
                        
                        Period: %s to %s
                        Account: %s
                        
                        Thanks,
                        BANK OF ANSH
                        """.formatted(userName, from, to, statementDto.getMaskedAccountNumber());

                String fileName = "statement_" + accountNumber + "_" + monthLabel + ".pdf";
                emailService.sendEmailWithAttachment(
                        userEmail,
                        subject,
                        body,
                        pdfBytes,
                        fileName
                );

                log.info("Statement emailed to {} for account {}", userEmail, accountNumber);
            } catch (Exception e) {
                log.error("Failed to send statement to {} for account {}", userEmail, accountNumber, e);
            }
        }
        log.info("Monthly statement job completed.");
    }

    @Async
    @Transactional(readOnly = true)
    public void sendMonthlyStatementsManually(YearMonth month){
        sendMonthlyStatementsInternal(month);
    }

    // Scheduling the
    @Scheduled(cron = "0 5 0 1 * ?", zone = "Asia/Kolkata")
    public void sendMonthlyStatements(){
        sendMonthlyStatementsInternal(YearMonth.now().minusMonths(1));
    }

}
