package net.engineerAnsh.BankApplication.Fraud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    // Spring automatically injects all FraudRule beans, Because we marked them as: @Component...
    private final List<FraudRule> fraudRules;

    public FraudEvaluationResult evaluate(TransactionContext context) {

        Set<String> reasons = new LinkedHashSet<>();
        FraudDecision finalResult = FraudDecision.SAFE;

        for (FraudRule rule : fraudRules) {

            FraudEvaluationResult result = rule.evaluate(context);

            // always log every triggered rules...
            if (result.getDecision() != FraudDecision.SAFE) {
                reasons.add(result.getReason()); // store the reason...
                log.warn(
                        "Fraud rule triggered: {} | Decision: {} | Reason: {}",
                        rule.getClass().getSimpleName(),
                        result.getDecision(),
                        result.getReason()
                );
            }

            // pick the most severe decision...
            if(result.getDecision().getPriority() > finalResult.getPriority()) {
                    finalResult = result.getDecision();
            }
        }
        return new FraudEvaluationResult(
                finalResult,
                String.join(" | ", reasons)
        );
    }
}
