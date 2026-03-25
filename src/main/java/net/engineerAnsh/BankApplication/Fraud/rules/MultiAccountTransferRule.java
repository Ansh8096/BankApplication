package net.engineerAnsh.BankApplication.Fraud.rules;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import net.engineerAnsh.BankApplication.Fraud.FraudEvaluationResult;
import net.engineerAnsh.BankApplication.Fraud.FraudRule;
import net.engineerAnsh.BankApplication.Fraud.TransactionContext;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Order(3)
public class MultiAccountTransferRule implements FraudRule {

    @Value("${fraud.multi-account-transfer-max-receivers}")
    private int MAX_RECEIVERS;

    @Value("${fraud.multi-account-transfer-time-window}")
    private int TIME_WINDOW_MINUTES;

    private final TransactionRepository transactionRepository;

    @Override
    public FraudEvaluationResult evaluate(TransactionContext context) {
        // Only apply for TRANSFER
        if (context.getTransactionType() != TransactionType.TRANSFER) {
            return FraudEvaluationResult.safe();
        }

        LocalDateTime since =
                context.getTimestamp().minusMinutes(TIME_WINDOW_MINUTES);

        int distinctReceivers =
                transactionRepository.countDistinctReceivers(
                        context.getAccountNumber(),
                        since
                );

        if (distinctReceivers >= MAX_RECEIVERS) {

            return FraudEvaluationResult.block(
                    "Multiple transfers to different accounts in short time"
            );
        }

        return FraudEvaluationResult.safe();
    }
}

// RULE DEFINITION:-
//  If:
//    User sends money to ≥ 3 different accounts
//    within last 2 minutes
//
//  Then:
//    BLOCK transaction