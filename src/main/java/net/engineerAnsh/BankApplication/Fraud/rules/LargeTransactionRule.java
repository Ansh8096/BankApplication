package net.engineerAnsh.BankApplication.Fraud.rules;

import net.engineerAnsh.BankApplication.Fraud.FraudEvaluationResult;
import net.engineerAnsh.BankApplication.Fraud.FraudRule;
import net.engineerAnsh.BankApplication.Fraud.TransactionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@Order(1)
public class LargeTransactionRule implements FraudRule {

    @Value("${fraud.large-transaction-threshold}")
    private BigDecimal LARGE_TX_THRESHOLD;

    @Override
    public FraudEvaluationResult evaluate(TransactionContext context) {

        if (context.getAmount().compareTo(LARGE_TX_THRESHOLD) >= 0) {

            return FraudEvaluationResult.suspicious(
                    "Large transaction detected: " + context.getAmount()
            );
        }

        return FraudEvaluationResult.safe();
    }
}

// Example rule:-
// Transaction > LARGE_TX_THRESHOLD → suspicious