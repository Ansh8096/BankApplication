package net.engineerAnsh.BankApplication.Services;

import net.engineerAnsh.BankApplication.Dto.StatementEmailContent.StatementEmailContent;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class AdminStatementService {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final StatementPdfService statementPdfService;
    private final EmailServiceImpl emailService;
    private final StatementEmailContentBuilder emailContentBuilder;

    @Async
    @Transactional(readOnly = true)
    public void sendMonthlyStatementsManually(YearMonth month) {
        log.info("Monthly statement job started manually by the ADMIN...");
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        log.info("Generating statements for period {} to {}", from, to);
        // We need to use this method for finding accounts in order to fetch users eagerly,
        // Also to prevent 'LazyInitializationException' in async thread...
        List<Account> accounts = accountRepository.findActiveAccountsWithUser(AccountStatus.ACTIVE);

        log.info("Found {} active accounts", accounts.size());

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

                // emailing...
                StatementEmailContent emailContent =
                        emailContentBuilder.build(
                                accountNumber,
                                userName,
                                from,
                                to,
                                statementDto.getMaskedAccountNumber(),
                                month
                        );

                emailService.sendEmailWithAttachment(
                        userEmail,
                        emailContent.getSubject(),
                        emailContent.getBody(),
                        pdfBytes,
                        emailContent.getFileName()
                );

                log.info("Statement emailed to {} for account {}", userEmail, accountNumber);
            } catch (Exception e) {
                log.error("Failed to send statement to {} for account {}", userEmail, accountNumber, e);
            }
        }
        log.info("Monthly statement job completed.");
    }

}
