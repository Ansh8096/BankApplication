package net.engineerAnsh.BankApplication.Services;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Dto.StatementEmailContent.StatementEmailContent;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.StatementJob;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.StatementJobStatus;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.StatementJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatementJobService {

    private final StatementJobRepository statementJobRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final StatementPdfService statementPdfService;
    private final EmailServiceImpl emailService;
    private final StatementEmailContentBuilder emailContentBuilder;

    @Value("${statement.max-retries}")
    private int maxRetryAttempts;

    private void generateAndSendStatement(
            Account account,
            int month,
            int year
    ) {
        String accountNumber = account.getAccountNumber();
        String userEmail = account.getUser().getEmail();
        String userName = account.getUser().getName();

        try {
            // Calculate statement period
            YearMonth reqMonth = YearMonth.of(year, month);
            LocalDate from = reqMonth.atDay(1);
            LocalDate to = reqMonth.atEndOfMonth();

            log.info("Generating statements for period {} to {}", from, to);

            // generate statement dto
            AccountStatementDto statementDto =
                    transactionService.generateMonthlyStatement(accountNumber, from, to);

            // generate pdf
            byte[] pdfBytes = statementPdfService.generatePdf(statementDto);

            // emailing...
            StatementEmailContent emailContent =
                    emailContentBuilder.build(
                            accountNumber,
                            userName,
                            from,
                            to,
                            statementDto.getMaskedAccountNumber(),
                            reqMonth
                    );

            emailService.sendEmailWithAttachment(
                    userEmail,
                    emailContent.getSubject(),
                    emailContent.getBody(),
                    pdfBytes,
                    emailContent.getFileName()
            );

            log.info("Statement emailed to {} for account {}", userEmail, accountNumber);
            saveSuccess(accountNumber, month, year);
        } catch (Exception e) {
            log.error("Failed to send statement to {} for account {}", userEmail, accountNumber, e);
            saveFailure(accountNumber, month, year, e.getMessage());
        }
    }

    @Transactional
    public void processStatementForAccount(
            @NotNull Account account,
            int month,
            int year
    ) {
        String accountNumber = account.getAccountNumber();

        Optional<StatementJob> existing =
                statementJobRepository
                        .findByAccountNumberAndMonthAndYear(
                                accountNumber, month, year);

        if (existing.isPresent()) {
            if (existing.get().getStatus() == StatementJobStatus.SUCCESS) {
                log.info("Statement already sent for account {}", accountNumber);
            }
            if (existing.get().getStatus() == StatementJobStatus.PERMANENTLY_FAILED) {
                log.info("This statement job has reached its maximum retries {}", accountNumber);
            }
            return;
        }
        generateAndSendStatement(account, month, year);
    }

    private void saveSuccess(String accountNumber, int month, int year) {
        StatementJob job = statementJobRepository
                .findByAccountNumberAndMonthAndYear(accountNumber, month, year)
                .orElse(null);

        if (job == null) {
            // First-time success...
            job = StatementJob.builder()
                    .accountNumber(accountNumber)
                    .month(month)
                    .year(year)
                    .retryCount(0)
                    .build();
        }

        job.setStatus(StatementJobStatus.SUCCESS);
        job.setErrorMessage(null);

        statementJobRepository.save(job);
    }

    private void saveFailure(
            String accountNumber,
            int month,
            int year,
            String errorMessage
    ) {
        StatementJob job = statementJobRepository
                .findByAccountNumberAndMonthAndYear(accountNumber, month, year)
                .orElse(null);

        if (job == null) {
            // First failure...
            job = StatementJob.builder()
                    .accountNumber(accountNumber)
                    .month(month)
                    .year(year)
                    .retryCount(0)
                    .status(StatementJobStatus.FAILED)
                    .errorMessage(errorMessage)
                    .build();
        } else {
            // Retry case...
            job.setRetryCount(job.getRetryCount() + 1);
            StatementJobStatus status = (job.getRetryCount() >= maxRetryAttempts)
                    ? StatementJobStatus.PERMANENTLY_FAILED
                    : StatementJobStatus.RETRIED;
            job.setStatus(status);
            job.setErrorMessage(errorMessage);
        }
        log.warn("Statement job failed for account {}, retryCount={}",
                accountNumber, job.getRetryCount());
        statementJobRepository.save(job);
    }

    @Transactional
    public void retryFailedJob(StatementJob job) {
        Account account = accountRepository
                .findByAccountNumberAndAccountStatus(job.getAccountNumber(), AccountStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        generateAndSendStatement(account, job.getMonth(), job.getYear());
    }

}




// Normal statement flow
//  processStatementForAccount()
//        ↓
//  generate PDF
//        ↓
//  send email
//        ↓
//  SUCCESS or FAILED (also increment retryCount) stored in DB
