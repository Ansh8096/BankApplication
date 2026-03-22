package net.engineerAnsh.BankApplication.Fraud;

public interface FraudRule {

    FraudEvaluationResult evaluate(TransactionContext context); // Every fraud rule must implement 'evaluate()'...

}