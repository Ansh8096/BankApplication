package net.engineerAnsh.BankApplication.Services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Dto.Statements.StatementRowDto;
import net.engineerAnsh.BankApplication.Dto.transaction.DepositRequest;
import net.engineerAnsh.BankApplication.Dto.transaction.TransactionResponse;
import net.engineerAnsh.BankApplication.Dto.transaction.TransferRequest;
import net.engineerAnsh.BankApplication.Dto.transaction.WithdrawRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    private TransactionResponse mapToTransactionResponse(Transaction txn) {
        // I'm able to set all the values in the constructor like this, because of the notations of constructors on 'TransactionResponse'...
        return new TransactionResponse(
                txn.getTransactionReference(),
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
                (txn.getRemark() != null && !txn.getRemark().isEmpty())
                        ? txn.getRemark()
                        : txn.getAmount() + " Sent to A/C " + AccountMaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber())
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
                (txn.getRemark() != null && !txn.getRemark().isEmpty())
                        ? txn.getRemark()
                        : txn.getAmount() + " Received from A/C " + AccountMaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber())
        );
    }

    // This method will return the 'TransactionResponse' of the 'transaction' if it exists, else return null...
    private TransactionResponse handleIdempotency(String clientTransactionId){
        return transactionRepository.findByClientTransactionId(clientTransactionId)
                .map(this::mapToTransactionResponse)
                .orElse(null);
    }

    private void validateClientTransactionId(String clientTransactionId) {
        if (clientTransactionId == null || clientTransactionId.isBlank()) {
            throw new RuntimeException("clientTransactionId is required");
        }
    }

    private void duplicateTransactionLog(String clientTransactionId){
        log.info("Duplicate transaction detected. Returning existing result. clientTransactionId={}",clientTransactionId);
    }

    @Transactional
    public TransactionResponse transferMoneyBetweenAccounts(TransferRequest request) throws AccessDeniedException {

        validateClientTransactionId(request.getClientTransactionId());

        TransactionResponse existingTransaction = handleIdempotency(request.getClientTransactionId());

        // return the previous result, if the same transaction is repeated...
        if (existingTransaction != null) {
            duplicateTransactionLog(request.getClientTransactionId());
            return existingTransaction;
        }

        // Finding the active accounts via their account number...
        Account from = findActiveAccountAndValidate(request.getFromAccountNumber()); // We also checks if the 'from' account belongs to loggedIn user or not...
        Account to = findTheActiveAccount(request.getToAccountNumber()); // Verifying if the 'to' account exists or not...

        // Prevent same-account transfer...
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        // Throw exception if given 'amount' <= zero or exceeding the maximum transfer limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.TRANSFER, request.getAmount());

        // Checking the daily limit of transfer...
        transactionLimitService.validateDailyTransactionLimit(TransactionType.TRANSFER, request.getAmount(), from.getAccountNumber());

        // Check balance using ledger...
        BigDecimal currentBalance = ledgerService.calculateAccountBalance(request.getFromAccountNumber());

        if (currentBalance.compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setAmount(request.getAmount());
        txn.setFromAccount(from);
        txn.setToAccount(to);
        txn.setRemark(request.getRemark());
        txn.setType(TransactionType.TRANSFER);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setClientTransactionId(request.getClientTransactionId()); // idempotency field...

        // Handle race condition :
        // (PB: Two identical requests arrive at the same time, Both pass the “not found” check, Then both try to insert)
        // Solution: Catch the DB exception and return the existing transaction...
        try {
            transactionRepository.save(txn); // save the transaction...
        } catch (DataIntegrityViolationException ex) {
            // Duplicate clientTransactionId detected
            Transaction transaction = transactionRepository
                    .findByClientTransactionId(request.getClientTransactionId())
                    .orElseThrow(() -> ex);
            return mapToTransactionResponse(transaction);
        }

        // Ledger entries
        ledgerService.recordTransfer(
                from,
                to,
                request.getAmount(),
                txn.getTransactionReference(),
                (request.getRemark() != null && !request.getRemark().isEmpty())
                        ? request.getRemark()
                        : "Transfer transaction"
        );

        // prepare kafka events...
        TransactionCompletedEvent senderEvent = buildSenderEvent(txn);
        TransactionCompletedEvent receiverEvent = buildReceiverEvent(txn);

        // Publish kafka events only after DB commit...
        // This block is used to delay our Kafka event publishing until after the database transaction successfully commits...
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        transactionEventProducer.publishTransactionCompleted(senderEvent);
                        transactionEventProducer.publishTransactionCompleted(receiverEvent);
                    }
                }
        );

        return mapToTransactionResponse(txn);

    }

    @Transactional
    public TransactionResponse depositMoneyToTheAccount(DepositRequest request) throws AccessDeniedException {

        validateClientTransactionId(request.getClientTransactionId());

        TransactionResponse existingTransaction = handleIdempotency(request.getClientTransactionId());

        // return the previous result, if the same transaction is repeated...
        if (existingTransaction != null) {
            duplicateTransactionLog(request.getClientTransactionId());
            return existingTransaction;
        }

        // We check if the 'to' account exists or not...
        Account account = findTheActiveAccount(request.getAccountNo());

        // Throw exception if given 'amount' <= zero or exceeding the maximum deposit limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.DEPOSIT, request.getAmount());

        // Checking the daily limit of deposit...
        transactionLimitService.validateDailyTransactionLimit(TransactionType.DEPOSIT, request.getAmount(), request.getAccountNo());

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setToAccount(account);
        txn.setType(TransactionType.DEPOSIT);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(request.getAmount());
        String description = (request.getRemark() != null && !request.getRemark().isEmpty()) ? request.getRemark() : "Deposited money";
        txn.setRemark(description);
        txn.setClientTransactionId(request.getClientTransactionId());

        try {
            // save the transaction:
            transactionRepository.save(txn);
        } catch (DataIntegrityViolationException ex) {
            // Duplicate clientTransactionId detected
            Transaction transaction = transactionRepository
                    .findByClientTransactionId(request.getClientTransactionId())
                    .orElseThrow(() -> ex);
            return mapToTransactionResponse(transaction);
        }

        // Ledger entries
        ledgerService.recordDeposit(
                account,
                request.getAmount(),
                txn.getTransactionReference(),
                txn.getRemark()
        );

        // prepare the kafka event...
        String userEmail = account.getUser().getEmail();
        TransactionCompletedEvent event = buildEvent(txn, userEmail);

        // Publish kafka events only after DB commit...
        // This block is used to delay our Kafka event publishing until after the database transaction successfully commits...
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        transactionEventProducer.publishTransactionCompleted(event);
                    }
                }
        );

        return mapToTransactionResponse(txn);
    }

    @Transactional
    public TransactionResponse withdrawMoneyFromTheAccount(WithdrawRequest request) throws AccessDeniedException {

        validateClientTransactionId(request.getClientTransactionId());

        TransactionResponse existingTransaction = handleIdempotency(request.getClientTransactionId());

        // return the previous result, if the same transaction is repeated...
        if (existingTransaction != null) {
            duplicateTransactionLog(request.getClientTransactionId());
            return existingTransaction;
        }

        // Finding the active account via account number...
        Account account = findActiveAccountAndValidate(request.getAccountNo()); // We also checks if the 'from' account belongs to loggedIn user or not...

        // Throw exception if given 'amount' <= zero or exceeding the maximum withdraw limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.WITHDRAW, request.getAmount());

        // Checking the daily limit of withdrawal...
        transactionLimitService.validateDailyTransactionLimit(TransactionType.WITHDRAW, request.getAmount(), request.getAccountNo());

        if(ledgerService.calculateAccountBalance(request.getAccountNo()).compareTo(request.getAmount()) < 0){
            throw new IllegalArgumentException("Insufficient balance");
        }

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setFromAccount(account);
        txn.setType(TransactionType.WITHDRAW);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(request.getAmount());
        txn.setRemark(WithdrawRequest.getRemark());
        txn.setClientTransactionId(request.getClientTransactionId());

        try {
            // save the transaction:
            transactionRepository.save(txn);
        } catch (DataIntegrityViolationException ex) {
            // Duplicate clientTransactionId detected
            Transaction transaction = transactionRepository
                    .findByClientTransactionId(request.getClientTransactionId())
                    .orElseThrow(() -> ex);
            return mapToTransactionResponse(transaction);
        }

        // Ledger entries
        ledgerService.recordWithdrawal(
                account,
                request.getAmount(),
                txn.getTransactionReference(),
                txn.getRemark()
        );

        // prepare the kafka event...
        String userEmail = account.getUser().getEmail();
        TransactionCompletedEvent event = buildEvent(txn, userEmail);

        // Publish kafka events only after DB commit...
        // This block is used to delay our Kafka event publishing until after the database transaction successfully commits...
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        transactionEventProducer.publishTransactionCompleted(event);
                    }
                }
        );

        return mapToTransactionResponse(txn);
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