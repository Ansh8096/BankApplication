package net.engineerAnsh.BankApplication.Fraud;

import lombok.Getter;

public enum FraudDecision {
    SAFE(0),            // Transaction allowed
    SUSPICIOUS(1),      // Allowed but flagged
    BLOCK(2),           // Transaction rejected
    FREEZE_ACCOUNT(3);  // Freeze account immediately

    @Getter
    private final int priority;

    FraudDecision(int priority) {
        this.priority = priority;
    }
}
