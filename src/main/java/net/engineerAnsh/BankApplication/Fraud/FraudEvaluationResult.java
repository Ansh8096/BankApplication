package net.engineerAnsh.BankApplication.Fraud;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FraudEvaluationResult {

    private FraudDecision decision;
    private String reason;

    public static FraudEvaluationResult safe() {
        return new FraudEvaluationResult(
                FraudDecision.SAFE,
                "Transaction is safe"
        );
    }

    public static FraudEvaluationResult suspicious(String reason) {
        return new FraudEvaluationResult(
                FraudDecision.SUSPICIOUS,
                reason
        );
    }

    public static FraudEvaluationResult block(String reason) {
        return new FraudEvaluationResult(
                FraudDecision.BLOCK,
                reason
        );
    }

    public static FraudEvaluationResult freeze(String reason) {
        return new FraudEvaluationResult(
                FraudDecision.FREEZE_ACCOUNT,
                reason
        );
    }
}