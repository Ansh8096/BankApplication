package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Fraud.FraudEvaluationResult;
import net.engineerAnsh.BankApplication.Fraud.TransactionContext;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class FraudEventBuilder {

    public FraudDetectedEvent buildFraudEvent(
            TransactionContext transactionContext,
            FraudEvaluationResult result) {
        return FraudDetectedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(transactionContext.getUserId())
                .email(transactionContext.getEmail())
                .accountNumber(transactionContext.getAccountNumber())
                .transactionType(transactionContext.getTransactionType())
                .amount(transactionContext.getAmount())
                .decision(result.getDecision())
                .reason(result.getReason())
                .timestamp(transactionContext.getTimestamp())
                .build();
    }

}
