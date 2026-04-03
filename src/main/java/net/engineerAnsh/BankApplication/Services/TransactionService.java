package net.engineerAnsh.BankApplication.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.account.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.kyc.KycStatus;
import net.engineerAnsh.BankApplication.Enum.outbox.OutboxEventType;
import net.engineerAnsh.BankApplication.Enum.transaction.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.transaction.TransactionType;
import net.engineerAnsh.BankApplication.Kafka.Builder.FraudEventBuilder;
import net.engineerAnsh.BankApplication.Fraud.FraudDecision;
import net.engineerAnsh.BankApplication.Fraud.FraudDetectionService;
import net.engineerAnsh.BankApplication.Fraud.FraudEvaluationResult;
import net.engineerAnsh.BankApplication.Fraud.TransactionContext;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import net.engineerAnsh.BankApplication.Utils.MaskingUtil;
import net.engineerAnsh.BankApplication.exception.FraudDetectedException;
import net.engineerAnsh.BankApplication.exception.KycNotVerifiedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final StatementBuilder statementBuilder;
    private final FraudDetectionService fraudDetectionService;
    private final TransactionExecutionService transactionExecutionService;
    private final TransactionLimitService transactionLimitService;
    private final LedgerService ledgerService;
    private final FraudEventBuilder fraudEventBuilder;
    private final OutboxEventService outboxEventService;
    private static final String KYC_REQ = "KYC verification required";
    private static final String NOT_OWNER_MSG = "The account doesn't belong to the logged-in user";

    public void validateBalance(String accountNo, BigDecimal amount) {

        BigDecimal balance = ledgerService.calculateAccountBalance(accountNo);

        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    public void validateLimits(TransactionType type, BigDecimal amount, String accountNo) {
        // Throw exception if given 'amount' <= zero or exceeding the maximum deposit limit...
        transactionLimitService.validatePerTransactionLimit(type, amount);
        // Checking the daily limit of deposit...
        transactionLimitService.validateDailyTransactionLimit(
                type,
                amount,
                accountNo
        );
    }

    public void validateTransferRequest(TransferRequest request, Account from) {

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

    private Account findAccountForRead(String accountNumber) {
        return accountRepository.findByAccountNumberAndAccountStatus(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active account found"));
    }

    private Account findAndValidateAccountForRead(String accountNumber) {
        Account account = findAccountForRead(accountNumber);
        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        String email = accountService.getEmailOfLoggedInUser();

        // Validate ownership...
        if (!account.getUser().getEmail().equals(email)) {
            log.error(NOT_OWNER_MSG);
            throw new AccessDeniedException(NOT_OWNER_MSG);
        }
        return account;
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
                MaskingUtil.maskAccountNumber(accountNumber),
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

    private void kycCheck(KycStatus status, String message) {
        if (status != KycStatus.APPROVED) {
            throw new KycNotVerifiedException(message);
        }
    }

    // This method will return the 'TransactionResponse' of the 'transaction' if it exists, else return null...
    private TransactionResponse handleIdempotency(String clientTransactionId) {
        return transactionRepository.findByClientTransactionId(clientTransactionId)
                .map(transactionExecutionService::mapToTransactionResponse)
                .orElse(null);
    }

    private void validateClientTransactionId(String clientTransactionId) {
        if (clientTransactionId == null || clientTransactionId.isBlank()) {
            throw new IllegalArgumentException("clientTransactionId is required");
        }
    }

    private TransactionResponse checkIdempotency(String clientTxnId) {
        TransactionResponse existing = handleIdempotency(clientTxnId);
        // return the previous result, if the same transaction is repeated...
        if (existing != null) {
            log.info("Duplicate transaction detected. Returning existing result. clientTransactionId={}", clientTxnId);
        }
        return existing;
    }

    private TransactionContext createTransactionContext(User user, Account sourceAccount, BigDecimal requestAmount, TransactionType transactionType) {
        return TransactionContext.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .accountNumber(sourceAccount.getAccountNumber())
                .amount(requestAmount)
                .transactionType(transactionType)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String createBlockTxn(
            Account fromAccount,
            Account toAccount,
            TransactionContext context,
            String clientTxnId,
            String reason
    ) {
        Transaction txn = Transaction.builder()
                .amount(context.getAmount())
                .remark("Blocked: " + reason)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .type(context.getTransactionType())
                .status(TransactionStatus.BLOCKED)
                .clientTransactionId(clientTxnId)
                .build();

        // Handle race condition :
        // (PB: Two identical requests arrive at the same time, Both pass the “not found” check, Then both try to insert)
        // Solution: Catch the DB exception and return the existing transaction...
        try {
            return transactionRepository.save(txn).getTransactionReference();
        } catch (DataIntegrityViolationException ex) {
            // returning the existing transaction, we can map it to transferResponse afterward...
            return transactionRepository
                    .findByClientTransactionId(clientTxnId)
                    .orElseThrow(() -> ex).getTransactionReference();
        }
    }

    private void handleFraud(
            Account sourceAccount,
            Account toAccount,
            FraudEvaluationResult result,
            TransactionContext context,
            String clientTxnId
    ) throws JsonProcessingException {

        String txnReference = null;
        // Handle BLOCK separately (needs txn creation)...
        if (result.getDecision() == FraudDecision.BLOCK) {
            txnReference = createBlockTxn(
                    sourceAccount,
                    toAccount,
                    context,
                    clientTxnId,
                    result.getReason()
            );
            log.error("🚫 BLOCKED FRAUD: {}", result.getReason());
        }

        if (result.getDecision() != FraudDecision.SAFE) {
            FraudDetectedEvent fraudEvent = fraudEventBuilder.buildFraudEvent(context, result, txnReference); // build fraud event...
            // Build and Save outBox event...
            OutboxEvent outboxFraudEvent = outboxEventService.buildOutboxEvent(fraudEvent, OutboxEventType.FRAUD_DETECTED);
            outboxEventService.publishOutBoxEvent(outboxFraudEvent);
        }

        switch (result.getDecision()) {
            case FREEZE_ACCOUNT:
                throw new FraudDetectedException(result.getReason());

            case SUSPICIOUS:
                log.warn("⚠️ Suspicious txn: {}", result.getReason());
                break;

            case BLOCK:
                throw new FraudDetectedException(result.getReason());

            case SAFE:
                break;
        }
    }

    public TransactionResponse withdrawMoneyFromTheAccount(WithdrawRequest request)
            throws AccessDeniedException, JsonProcessingException {

        // validate client txn id...
        validateClientTransactionId(request.getClientTransactionId());

        // Check Idempotency...
        TransactionResponse existing = checkIdempotency(request.getClientTransactionId());
        if (existing != null) return existing;

        // Step-1 fetching the account for read only (NO LOCK)...
        Account account = findAndValidateAccountForRead(request.getAccountNo()); // We also checks if the 'from' account belongs to loggedIn user or not...

        // KYC check...
        kycCheck(account.getUser().getKycStatus(), KYC_REQ);

        // Business validations:-
        validateLimits(TransactionType.WITHDRAW, request.getAmount(), request.getAccountNo()); // Validate withdraw limits
        validateBalance(account.getAccountNumber(), request.getAmount()); // Balance check

        // STEP 2: FRAUD CHECK:--
        TransactionContext context = createTransactionContext(
                account.getUser(),
                account,
                request.getAmount(),
                TransactionType.WITHDRAW
        );
        FraudEvaluationResult result = fraudDetectionService.evaluate(context);

        // Handle fraud before transaction...
        handleFraud(account, null, result, context, request.getClientTransactionId());

        // Call transactional method...
        return transactionExecutionService.executeWithdraw(request, account);
    }

    public TransactionResponse depositMoneyToTheAccount(DepositRequest request)
            throws AccessDeniedException, JsonProcessingException {

        // validate client txn id...
        validateClientTransactionId(request.getClientTransactionId());

        // Check Idempotency...
        TransactionResponse existing = checkIdempotency(request.getClientTransactionId());
        if (existing != null) return existing;

        // Validate the deposit limits...
        validateLimits(TransactionType.DEPOSIT, request.getAmount(), request.getAccountNo());

        // Step-1 fetching the account for read only (NO LOCK)
        Account toAcc = findAccountForRead(request.getAccountNo()); // We also checks if the 'from' account belongs to loggedIn user or not...

        // verifying the kyc status of account holder...
        kycCheck(toAcc.getUser().getKycStatus(), KYC_REQ);

        // STEP 2: fraud check:--
        TransactionContext transactionContext =
                createTransactionContext(toAcc.getUser(), toAcc, request.getAmount(), TransactionType.DEPOSIT);
        FraudEvaluationResult result = fraudDetectionService.evaluate(transactionContext); // fraud detection.
        handleFraud(null, toAcc, result, transactionContext, request.getClientTransactionId()); // Handle fraud before transaction...

        // STEP 3: Call transactional method...
        return transactionExecutionService.executeDeposit(request, toAcc);
    }

    public TransactionResponse transferMoneyBetweenAccounts(TransferRequest request)
            throws AccessDeniedException, JsonProcessingException {

        validateClientTransactionId(request.getClientTransactionId());

        // Idempotency check...
        TransactionResponse existing = checkIdempotency(request.getClientTransactionId());
        if (existing != null) return existing;

        // Step-1 fetching accounts to read only (NO LOCK)...
        Account fromAcc = findAndValidateAccountForRead(request.getFromAccountNumber());
        Account toAcc = findAccountForRead(request.getToAccountNumber());

        // verifying the kyc status of a user...
        kycCheck(fromAcc.getUser().getKycStatus(), KYC_REQ);
        kycCheck(toAcc.getUser().getKycStatus(), "Receiver KYC incomplete");

        // Business validations:-
        validateTransferRequest(request, fromAcc); // It will apply all the necessary validations to request...

        // STEP 2: FRAUD CHECK:--
        TransactionContext transactionContext = createTransactionContext(
                fromAcc.getUser(),
                fromAcc,
                request.getAmount(),
                TransactionType.TRANSFER
        );
        FraudEvaluationResult result = fraudDetectionService.evaluate(transactionContext); // fraud detection...
        handleFraud(fromAcc, toAcc, result, transactionContext, request.getClientTransactionId()); // Handle fraud before txn...

        // Step 3 call transactional method...
        return transactionExecutionService.executeTransfer(request, fromAcc, toAcc);
    }

    @Transactional(readOnly = true)
    public AccountStatementDto generateStatement(
            String accountNumber,
            LocalDate from,
            LocalDate to
    ) {
        // Validate account ownership (API use case)...
        Account account = findAndValidateAccountForRead(accountNumber);

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