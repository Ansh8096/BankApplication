package net.engineerAnsh.BankApplication.Services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Dto.Statements.StatementRowDto;
import net.engineerAnsh.BankApplication.Dto.transaction.TransactionResponse;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Kafka.Producer.TransactionEventProducer;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import net.engineerAnsh.BankApplication.Utils.AccountMaskingUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final StatementBuilder statementBuilder;
    private final TransactionLimitService transactionLimitService;
    private final TransactionEventProducer transactionEventProducer;
    private final LedgerService ledgerService;

    private Account findTheActiveAccount(String accountNumber) {
        return accountRepository.findByAccountNumberAndAccountStatus(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active account found"));
    }

    public Account findActiveAccountAndValidate(String accountNumber) throws AccessDeniedException {
        String email = accountService.getEmailOfLoggedInUser();
        Account account = accountRepository.findByAccountNumberAndAccountStatus(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Account not found or is not active"));
        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        if (!account.getUser().getEmail().equals(email)) {
            log.error(AccountService.getNot_owner_msg());
            throw new AccessDeniedException(AccountService.getNot_owner_msg());
        }
        return account;
    }

    // I will be using this method when we want to fetch transaction by reference number...
    private TransactionResponse mapToTransactionResponse(Transaction txn) {
        // I'm able to set all the values in the constructor like this, because of the notations of constructors on 'TransactionResponse'...
        return new TransactionResponse(
                txn.getId(),
                txn.getFromAccount() != null
                        ? AccountMaskingUtil.maskAccountNumber(
                        txn.getFromAccount().getAccountNumber())
                        : null,
                txn.getToAccount() != null
                        ? AccountMaskingUtil.maskAccountNumber(
                        txn.getToAccount().getAccountNumber())
                        : null,
                txn.getAmount(),
                txn.getType(),
                txn.getStatus(),
                txn.getRemark(),
                txn.getCreatedAt()
        );
    }

    private BigDecimal calculateOpeningBalanceForTransactions(
            String accountNumber,
            LocalDate from
    ) {

        // this method will fetch all the transactions before 'from' date, so that we can easily calculate the opening balance...
        List<Transaction> previousTransactionsBeforeDate = transactionRepository.findAllSuccessfulTransactionsBeforeDate(
                accountNumber, from.atStartOfDay());

        previousTransactionsBeforeDate.sort(
                Comparator.comparing(Transaction::getCreatedAt)
        );

        BigDecimal openingBalance = BigDecimal.ZERO;

        // Maintaining the opening balance according to the credit or debit transactions...
        for (Transaction tx : previousTransactionsBeforeDate) {

            // Ignore FAILED / REVERSED transactions...
            if (tx.getStatus() != TransactionStatus.SUCCESS) {
                continue;
            }
            if (tx.getFromAccount() != null && tx.getFromAccount().getAccountNumber().equals(accountNumber)) {
                // debit...
                openingBalance = openingBalance.subtract(tx.getAmount());
            } else if (tx.getToAccount() != null && tx.getToAccount().getAccountNumber().equals(accountNumber)) {
                // credit...
                openingBalance = openingBalance.add(tx.getAmount());
            }
        }

        log.info("Opening balance = {}", openingBalance);
        return openingBalance;
    }

    private AccountStatementDto generateStatementInternal(
            Account account,
            String accountNumber,
            LocalDate from,
            LocalDate to
    ) {

        // Calculating the opening balance (accountBalance on that time),before from day...
        BigDecimal openingBalance = calculateOpeningBalanceForTransactions(accountNumber, from);

        // Define Time window...
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); // Exclusive...

        // Fetch all the valid transactions within the given time...
        List<Transaction> transactions = transactionRepository.findStatementTransactions(accountNumber, start, end);

        // Build rows of statements...
        List<StatementRowDto> statementRows = statementBuilder.buildStatementRows(
                transactions,
                accountNumber,
                openingBalance
        );

        //  Return object of the accountStatement DTO...
        return new AccountStatementDto(
                "BANK OF ANSH",
                account.getUser().getName(),
                AccountMaskingUtil.maskAccountNumber(accountNumber),
                account.getAccountType().toString(),
                account.getIfscCode(),
                from,
                to,
                openingBalance,
                statementRows,
                statementRows.isEmpty()
                        ? openingBalance
                        : statementRows.get(statementRows.size() - 1).getBalance()
        );
    }

    private TransactionCompletedEvent buildEvent(Transaction txn, String userEmail) {
        log.info("Started building the kafka event...");
        return new TransactionCompletedEvent(
                UUID.randomUUID().toString(),
                txn.getTransactionReference(),
                txn.getType().name(),
                txn.getStatus().name(),
                txn.getAmount(),
                txn.getFromAccount() != null
                        ? AccountMaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber())
                        : null,
                txn.getToAccount() != null
                        ? AccountMaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber())
                        : null,
                txn.getCreatedAt(),
                userEmail,
                txn.getRemark()
        );
    }


    private TransactionCompletedEvent buildSenderEvent(Transaction txn) {
        log.info("Started building the kafka event for transaction completed for sender...");
        return new TransactionCompletedEvent(
                UUID.randomUUID().toString(),
                txn.getTransactionReference(),
                "TRANSFER_SENT",
                txn.getStatus().name(),
                txn.getAmount(),
                AccountMaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber()),
                AccountMaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber()),
                txn.getCreatedAt(),
                txn.getFromAccount().getUser().getEmail(),
                (txn.getRemark() != null && !txn.getRemark().isEmpty()) ? txn.getRemark()
                        : "Sent to A/C " + AccountMaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber())
        );
    }

    private TransactionCompletedEvent buildReceiverEvent(Transaction txn) {
        log.info("Started building the kafka event for transaction completed for receiver...");
        return new TransactionCompletedEvent(
                UUID.randomUUID().toString(),
                txn.getTransactionReference(),
                "TRANSFER_RECEIVED",
                txn.getStatus().name(),
                txn.getAmount(),
                AccountMaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber()),
                AccountMaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber()),
                txn.getCreatedAt(),
                txn.getToAccount().getUser().getEmail(),
                (txn.getRemark() != null && !txn.getRemark().isEmpty()) ? txn.getRemark()
                        : "Received from A/C " + AccountMaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber())
        );
    }

    @Transactional
    public void transferMoneyBetweenAccounts(
            String fromAccountNo,
            String toAccountNo,
            BigDecimal amount,
            String remark
    ) throws AccessDeniedException {

        // Finding the active accounts via their account number...
        Account from = findActiveAccountAndValidate(fromAccountNo); // We also checks if the 'from' account belongs to loggedIn user or not...
        Account to = findTheActiveAccount(toAccountNo); // Verifying if the 'to' account exists or not...

        // Prevent same-account transfer...
        if (fromAccountNo.equals(toAccountNo)) {
            throw new RuntimeException("Cannot transfer to same account");
        }

        // Throw exception if given 'amount' <= zero or exceeding the maximum transfer limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.TRANSFER, amount);

        // Checking the daily limit of transfer...
        transactionLimitService.validateDailyTransactionLimit(TransactionType.TRANSFER, amount, from.getAccountNumber());

        // Check balance using ledger...
        BigDecimal currentBalance = ledgerService.calculateAccountBalance(fromAccountNo);

        if(currentBalance.compareTo(amount) < 0){
            throw new RuntimeException("Insufficient balance");
        }

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setAmount(amount);
        txn.setFromAccount(from);
        txn.setToAccount(to);
        txn.setRemark(remark);
        txn.setType(TransactionType.TRANSFER);
        txn.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(txn); // save the transaction...

        // Ledger entries
        ledgerService.recordTransfer(
                from,
                to,
                amount,
                txn.getTransactionReference(),
                (remark != null && !remark.isEmpty()) ? remark : "Transfer transaction"
        );

        // publish the kafka event...
        TransactionCompletedEvent senderEvent = buildSenderEvent(txn);
        TransactionCompletedEvent receiverEvent = buildReceiverEvent(txn);
        transactionEventProducer.publishTransactionCompleted(senderEvent);
        transactionEventProducer.publishTransactionCompleted(receiverEvent);
    }

    @Transactional
    public void depositMoneyToTheAccount(String toAccountNo,
                                         BigDecimal amount,
                                         String remark) throws AccessDeniedException {

        // We check if the 'to' account exists or not...
        Account account = findTheActiveAccount(toAccountNo);

        // Throw exception if given 'amount' <= zero or exceeding the maximum deposit limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.DEPOSIT, amount);

        // Checking the daily limit of deposit...
        transactionLimitService.validateDailyTransactionLimit(TransactionType.DEPOSIT, amount, toAccountNo);

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setToAccount(account);
        txn.setType(TransactionType.DEPOSIT);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        String description = (remark != null && !remark.isEmpty()) ? remark : "Deposited money";
        txn.setRemark(description);

        // save the transaction:
        transactionRepository.save(txn);

        // Ledger entries
        ledgerService.recordDeposit(
                account,
                amount,
                txn.getTransactionReference(),
                txn.getRemark()
        );

        // publish the kafka event...
        String userEmail = account.getUser().getEmail();
        TransactionCompletedEvent event = buildEvent(txn, userEmail);
        transactionEventProducer.publishTransactionCompleted(event);

    }

    @Transactional
    public void withdrawMoneyFromTheAccount(String fromAccountNo,
                                            BigDecimal amount,
                                            String remark) throws AccessDeniedException {

        // Finding the active account via account number...
        Account account = findActiveAccountAndValidate(fromAccountNo); // We also checks if the 'from' account belongs to loggedIn user or not...

        // Throw exception if given 'amount' <= zero or exceeding the maximum withdraw limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.WITHDRAW, amount);

        // Checking the daily limit of withdrawal...
        transactionLimitService.validateDailyTransactionLimit(TransactionType.WITHDRAW, amount, fromAccountNo);

        // Throw exception if given amount is greater than the account balance...
        if (ledgerService.calculateAccountBalance(fromAccountNo).compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient Balance");
        }

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setFromAccount(account);
        txn.setType(TransactionType.WITHDRAW);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        if (remark != null && !remark.isEmpty()) {
            txn.setRemark(remark);
        }

        // save the transaction:
        transactionRepository.save(txn);

        // Ledger entries
        ledgerService.recordWithdrawal(
                account,
                amount,
                txn.getTransactionReference(),
                txn.getRemark()
        );

        // publish the kafka event...
        String userEmail = account.getUser().getEmail();
        TransactionCompletedEvent event = buildEvent(txn, userEmail);
        transactionEventProducer.publishTransactionCompleted(event);
    }

    @Transactional(readOnly = true)
    public AccountStatementDto generateStatement(
            String accountNumber,
            LocalDate from,
            LocalDate to
    ) {
        // Validate account ownership (API use case)...
        Account account = findActiveAccountAndValidate(accountNumber);

        // Calling the internal implementation of the generating statement...
        return generateStatementInternal(account, accountNumber, from, to);
    }

    @Transactional(readOnly = true)
    public AccountStatementDto generateMonthlyStatement(
            String accountNumber,
            LocalDate from,
            LocalDate to
    ) {
        // NO ownership validation (scheduler use case)...
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("An error occurred..."));

        // Calling the internal implementation of the generating statement...
        return generateStatementInternal(account, accountNumber, from, to);
    }

}