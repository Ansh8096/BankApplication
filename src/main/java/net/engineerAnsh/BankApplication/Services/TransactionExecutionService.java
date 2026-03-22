package net.engineerAnsh.BankApplication.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionExecutionService {


    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    private Account findAccountWithLock(String accountNumber) {
        return accountRepository.findAccountForUpdate(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active account found"));
    }

    public TransactionResponse mapToTransactionResponse(Transaction txn) {
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

    private TransactionCompletedEvent buildEvent(Transaction txn, String userEmail) {
        log.info("Building transaction event: type={}, ref={}", txn.getType(), txn.getTransactionReference());
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
        log.info("Building sender event (transfer transaction): ref={}", txn.getTransactionReference());
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
        log.info("Building receiver event (transfer transaction):  ref={}", txn.getTransactionReference());
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


    private String resolveRemark(String remark, String defaultText) {
        return (remark != null && !remark.isEmpty()) ? remark : defaultText;
    }

    private void saveTransactionEvent(Transaction txn, String email)
            throws JsonProcessingException {

        TransactionCompletedEvent event = buildEvent(txn, email);
        saveOutboxEvent(event);
    }

    // creating a custom wrapper...
    record AccountPair(Account from, Account to){}

    private AccountPair getLockedAccountsForTransfer(Account fromAcc, Account toAcc) {
        // The deadlock condition occurs, Suppose when the two users transferring at same time:
        // Thread A: transfers A -> B, locks 'A' waits for 'B'
        // Thread B: transfers B -> A, locks 'B' waits for 'A'
        // I avoid this condition by always locking the account that is first created...
        Account from;
        Account to;
        if (fromAcc.getId() < toAcc.getId()) {
            from = findAccountWithLock(fromAcc.getAccountNumber());
            to = findAccountWithLock(toAcc.getAccountNumber());
        } else {
            to = findAccountWithLock(toAcc.getAccountNumber());
            from = findAccountWithLock(fromAcc.getAccountNumber());
        }
        return new AccountPair(from,to);
    }

    @Transactional
    public TransactionResponse executeWithdraw(WithdrawRequest request, Account account)
            throws JsonProcessingException {

        // lock the account...
        Account lockedAccount = findAccountWithLock(account.getAccountNumber());

        // resolving remark...
        String remark = resolveRemark(request.getRemark(), "ATM Withdrawal");

        // creating new txn...
        Transaction txn = createTransaction(
                request.getAmount(),
                request.getClientTransactionId(),
                lockedAccount,
                null,
                TransactionType.WITHDRAW,
                remark
        );

        // create ledger entry...
        ledgerService.recordWithdrawal(
                lockedAccount,
                request.getAmount(),
                txn.getTransactionReference(),
                txn.getRemark()
        );

        // building and saving the withdrawal Outbox event...
        saveTransactionEvent(txn, lockedAccount.getUser().getEmail());

        return mapToTransactionResponse(txn);
    }

    @Transactional
    public TransactionResponse executeDeposit(DepositRequest request, Account account)
            throws JsonProcessingException {

        // lock the account...
        Account locked = findAccountWithLock(account.getAccountNumber());

        // resolving remark...
        String remark = resolveRemark(request.getRemark(), "Deposited money");

        // create txn...
        Transaction txn = createTransaction(
                request.getAmount(),
                request.getClientTransactionId(),
                null,
                locked,
                TransactionType.DEPOSIT,
                remark
        );

        // create ledger entry...
        ledgerService.recordDeposit(
                locked,
                request.getAmount(),
                txn.getTransactionReference(),
                txn.getRemark()
        );

        // prepare and saving the outbox event for deposit...
        saveTransactionEvent(txn, locked.getUser().getEmail());

        return mapToTransactionResponse(txn);
    }

    @Transactional
    public TransactionResponse executeTransfer(
            TransferRequest request,
            Account fromAcc,
            Account toAcc
    ) throws JsonProcessingException {

        // Lock accounts now...
        AccountPair accountPair = getLockedAccountsForTransfer(fromAcc, toAcc);
        Account from = accountPair.from();
        Account to   = accountPair.to();

        // resolve remark...
        String remark = resolveRemark(request.getRemark(), "Transferred money");

        Transaction txn = createTransaction(
                request.getAmount(),
                request.getClientTransactionId(),
                from,
                to,
                TransactionType.TRANSFER,
                remark
        );

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
}
