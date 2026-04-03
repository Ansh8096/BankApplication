package net.engineerAnsh.BankApplication.Kafka.Builder;

import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Utils.MaskingUtil;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
public class TransactionEventBuilder {

    public TransactionCompletedEvent buildTxnEvent(Transaction txn, String userEmail) {
        log.info("Building transaction event: type={}, ref={}", txn.getType(), txn.getTransactionReference());
        return new TransactionCompletedEvent(
                UUID.randomUUID().toString(),
                txn.getTransactionReference(),
                txn.getType().name(),
                txn.getStatus().name(),
                txn.getAmount(),
                txn.getFromAccount() != null
                        ? MaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber())
                        : null,
                txn.getToAccount() != null
                        ? MaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber())
                        : null,
                txn.getCreatedAt(),
                userEmail,
                txn.getRemark()
        );
    }

    public TransactionCompletedEvent buildSenderTxnEvent(Transaction txn) {
        log.info("Building sender event (transfer transaction): ref={}", txn.getTransactionReference());
        return new TransactionCompletedEvent(
                UUID.randomUUID().toString(),
                txn.getTransactionReference(),
                "TRANSFER_SENT",
                txn.getStatus().name(),
                txn.getAmount(),
                MaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber()),
                MaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber()),
                txn.getCreatedAt(),
                txn.getFromAccount().getUser().getEmail(),
                (txn.getRemark() != null && !txn.getRemark().isEmpty())
                        ? txn.getRemark()
                        : txn.getAmount() + " Sent to A/C " + MaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber())
        );
    }

    public TransactionCompletedEvent buildReceiverTxnEvent(Transaction txn) {
        log.info("Building receiver event (transfer transaction):  ref={}", txn.getTransactionReference());
        return new TransactionCompletedEvent(
                UUID.randomUUID().toString(),
                txn.getTransactionReference(),
                "TRANSFER_RECEIVED",
                txn.getStatus().name(),
                txn.getAmount(),
                MaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber()),
                MaskingUtil.maskAccountNumber(txn.getToAccount().getAccountNumber()),
                txn.getCreatedAt(),
                txn.getToAccount().getUser().getEmail(),
                (txn.getRemark() != null && !txn.getRemark().isEmpty())
                        ? txn.getRemark()
                        : txn.getAmount() + " Received from A/C " + MaskingUtil.maskAccountNumber(txn.getFromAccount().getAccountNumber())
        );
    }

}
