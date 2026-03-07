package net.engineerAnsh.BankApplication.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Enum.*;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.OutboxEventRepository;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import net.engineerAnsh.BankApplication.Utils.AccountMaskingUtil;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    private Account findTheActiveAccount(String accountNumber) {
        return accountRepository.findAccountForUpdate(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active account found"));
    }

    private void validateTheAccount(String userEmail) {
        String email = accountService.getEmailOfLoggedInUser();
        if (!userEmail.equals(email)) {
            log.error(AccountService.getNot_owner_msg());
            throw new AccessDeniedException(AccountService.getNot_owner_msg());
        }
    }

    private Account findAndValidateTheActiveAccountToReadOnly(String accountNumber) {
        Account account = accountRepository.findByAccountNumberAndAccountStatus(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active account found"));

        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        validateTheAccount(account.getUser().getEmail());
        return account;
    }

    public Account findActiveAccountAndValidate(String accountNumber) throws AccessDeniedException {
        Account account = findTheActiveAccount(accountNumber);

        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        validateTheAccount(account.getUser().getEmail());
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
        log.info("Building transaction event. reference={}", txn.getTransactionReference());
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
        log.info("Building Transfer event. reference={}", txn.getTransactionReference());
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
    private TransactionResponse handleIdempotency(String clientTransactionId) {
        return transactionRepository.findByClientTransactionId(clientTransactionId)
                .map(this::mapToTransactionResponse)
                .orElse(null);
    }

    private void validateClientTransactionId(String clientTransactionId) {
        if (clientTransactionId == null || clientTransactionId.isBlank()) {
            throw new IllegalArgumentException("clientTransactionId is required");
        }
    }

    private void duplicateTransactionLog(String clientTransactionId) {
        log.info("Duplicate transaction detected. Returning existing result. clientTransactionId={}", clientTransactionId);
    }

    private TransactionResponse checkIdempotency(String clientTxnId) {
        TransactionResponse existing = handleIdempotency(clientTxnId);
        // return the previous result, if the same transaction is repeated...
        if (existing != null) {
            duplicateTransactionLog(clientTxnId);
        }
        return existing;
    }

    private void validateBalance(String accountNo, BigDecimal amount) {

        BigDecimal balance = ledgerService.calculateAccountBalance(accountNo);

        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    private void validateTransferRequest(TransferRequest request, Account from) {

        // Prevent same-account transfer...
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        // Throw exception if given 'amount' <= zero or exceeding the maximum transfer limit...
        transactionLimitService.validatePerTransactionLimit(
                TransactionType.TRANSFER,
                request.getAmount()
        );

        // Checking the daily limit of transfer...
        transactionLimitService.validateDailyTransactionLimit(
                TransactionType.TRANSFER,
                request.getAmount(),
                from.getAccountNumber()
        );

        validateBalance(from.getAccountNumber(), request.getAmount());

    }

    private Transaction createTransaction(
            BigDecimal amount,
            String clientTxnId,
            Account from,
            Account to,
            TransactionType type,
            String remark
    ) {

        // Creating new transaction object to save the record in the table...
        Transaction txn = Transaction.builder()
                .amount(amount)
                .fromAccount(from)
                .toAccount(to)
                .remark(remark)
                .type(type)
                .status(TransactionStatus.SUCCESS)
                .clientTransactionId(clientTxnId) // idempotency field...
                .build();

        // Handle race condition :
        // (PB: Two identical requests arrive at the same time, Both pass the “not found” check, Then both try to insert)
        // Solution: Catch the DB exception and return the existing transaction...
        try {
            transactionRepository.save(txn);
        } catch (DataIntegrityViolationException ex) {
            // returning the existing transaction, we can map it to transferResponse afterwards...
            return transactionRepository
                    .findByClientTransactionId(clientTxnId)
                    .orElseThrow(() -> ex);
        }

        return txn;
    }


    private void saveOutboxEvent(Object event)
            throws JsonProcessingException {

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventType(OutboxEventType.TRANSACTION_COMPLETED)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    private void saveTransferEvents(Transaction txn)
            throws JsonProcessingException {

        TransactionCompletedEvent senderEvent = buildSenderEvent(txn);
        TransactionCompletedEvent receiverEvent = buildReceiverEvent(txn);

        saveOutboxEvent(senderEvent);
        saveOutboxEvent(receiverEvent);
    }

    private void validateLimits(TransactionType type, BigDecimal amount, String accountNo) {
        // Throw exception if given 'amount' <= zero or exceeding the maximum deposit limit...
        transactionLimitService.validatePerTransactionLimit(type, amount);
        // Checking the daily limit of deposit...
        transactionLimitService.validateDailyTransactionLimit(
                type,
                amount,
                accountNo
        );
    }

    private String resolveRemark(String remark, String defaultText) {
        return (remark != null && !remark.isEmpty()) ? remark : defaultText;
    }

    private void saveTransactionEvent(Transaction txn, String email)
            throws JsonProcessingException {

        TransactionCompletedEvent event = buildEvent(txn, email);
        saveOutboxEvent(event);
    }

    @Transactional
    public TransactionResponse transferMoneyBetweenAccounts(TransferRequest request) throws AccessDeniedException, JsonProcessingException {

        validateClientTransactionId(request.getClientTransactionId());

        // Check Idempotency...
        TransactionResponse existing = checkIdempotency(request.getClientTransactionId());
        if (existing != null) return existing;

        // Finding the active accounts via their account number...
        Account from = findActiveAccountAndValidate(request.getFromAccountNumber()); // We also checks if the 'from' account belongs to loggedIn user or not...
        Account to = findTheActiveAccount(request.getToAccountNumber()); // Verifying if the 'to' account exists or not...

        // It will apply all the necessary validations to request...
        validateTransferRequest(request, from);

        // resolving remark...
        String remark = resolveRemark(request.getRemark(), "Transferred money");

        // Creating the transaction...
        Transaction txn = createTransaction(request.getAmount(), request.getClientTransactionId(), from, to, TransactionType.TRANSFER, remark);

        // Ledger entries
        ledgerService.recordTransfer(
                from,
                to,
                request.getAmount(),
                txn.getTransactionReference(),
                remark
        );

        saveTransferEvents(txn);

        return mapToTransactionResponse(txn);
    }

    @Transactional
    public TransactionResponse depositMoneyToTheAccount(DepositRequest request) throws AccessDeniedException, JsonProcessingException {

        validateClientTransactionId(request.getClientTransactionId());

        // Check Idempotency...
        TransactionResponse existing = checkIdempotency(request.getClientTransactionId());
        if (existing != null) return existing;

        // We check if the 'to' account exists or not...
        Account account = findTheActiveAccount(request.getAccountNo());

        // Validate the deposit limits...
        validateLimits(TransactionType.DEPOSIT, request.getAmount(), request.getAccountNo());

        // Resolving the remark...
        String remark = resolveRemark(request.getRemark(), "Deposited money");

        // Creating new transaction object to save the record in the table...
        Transaction txn = createTransaction(
                request.getAmount(),
                request.getClientTransactionId(),
                null,
                account,
                TransactionType.DEPOSIT,
                remark
        );

        // Saving Ledger entry...
        ledgerService.recordDeposit(
                account,
                request.getAmount(),
                txn.getTransactionReference(),
                txn.getRemark()
        );

        // Preparing and saving the withdrawal outbox event...
        saveTransactionEvent(txn, account.getUser().getEmail());

        return mapToTransactionResponse(txn);
    }

    @Transactional
    public TransactionResponse withdrawMoneyFromTheAccount(WithdrawRequest request) throws AccessDeniedException, JsonProcessingException {

        validateClientTransactionId(request.getClientTransactionId());

        // Check Idempotency...
        TransactionResponse existing = checkIdempotency(request.getClientTransactionId());
        if (existing != null) return existing;

        // Finding the active account via account number...
        Account account = findActiveAccountAndValidate(request.getAccountNo()); // We also checks if the 'from' account belongs to loggedIn user or not...

        // Validate withdraw limits...
        validateLimits(TransactionType.WITHDRAW, request.getAmount(), request.getAccountNo());

        // Validating balance...
        validateBalance(account.getAccountNumber(), request.getAmount());

        // Resolving the remark...
        String remark = resolveRemark(request.getRemark(), "ATM Withdrawal");

        // Creating new transaction object to save the record in the table...
        Transaction txn = createTransaction(
                request.getAmount(),
                request.getClientTransactionId(),
                account,
                null,
                TransactionType.WITHDRAW,
                remark
        );

        // Ledger entries
        ledgerService.recordWithdrawal(
                account,
                request.getAmount(),
                txn.getTransactionReference(),
                txn.getRemark()
        );

        // Preparing and saving the withdrawal outbox event...
        saveTransactionEvent(txn, account.getUser().getEmail());

        return mapToTransactionResponse(txn);
    }

    @Transactional(readOnly = true)
    public AccountStatementDto generateStatement(
            String accountNumber,
            LocalDate from,
            LocalDate to
    ) {
        // Validate account ownership (API use case)...
        Account account = findAndValidateTheActiveAccountToReadOnly(accountNumber);

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
                .orElseThrow(() -> new EntityNotFoundException("Account Not Found..."));

        // Calling the internal implementation of the generating statement...
        return generateStatementInternal(account, accountNumber, from, to);
    }

}