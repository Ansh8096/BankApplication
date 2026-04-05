package net.engineerAnsh.BankApplication.Enum.outbox;

public enum OutboxStatus {

    PENDING,
    RETRIED,
    PROCESSING,
    FAILED,
    PROCESSED
}
