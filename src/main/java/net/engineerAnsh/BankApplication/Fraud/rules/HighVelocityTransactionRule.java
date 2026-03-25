package net.engineerAnsh.BankApplication.Fraud.rules;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Config.FraudVelocityProperties;
import net.engineerAnsh.BankApplication.Fraud.FraudEvaluationResult;
import net.engineerAnsh.BankApplication.Fraud.FraudRule;
import net.engineerAnsh.BankApplication.Fraud.TransactionContext;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Order(2)
public class HighVelocityTransactionRule implements FraudRule {

    private final TransactionRepository transactionRepository;
    private final FraudVelocityProperties fraudVelocityProperties;

    @Override
    public FraudEvaluationResult evaluate(TransactionContext context) {
        int maxAllowed =
                fraudVelocityProperties
                        .getVelocityLimits()
                        .getOrDefault(context.getTransactionType(), 5);

        LocalDateTime oneMinuteAgo =
                context.getTimestamp().minusMinutes(1);

        int recentTransactions =
                transactionRepository.countTransactionsSince(
                        context.getAccountNumber(),
                        context.getTransactionType(),
                        oneMinuteAgo
                );

        if (recentTransactions > maxAllowed) {
            return FraudEvaluationResult.freeze(
                    "Too many " + context.getTransactionType()
                            + " transactions in short time"
            );
        }

        return FraudEvaluationResult.safe();
    }
}