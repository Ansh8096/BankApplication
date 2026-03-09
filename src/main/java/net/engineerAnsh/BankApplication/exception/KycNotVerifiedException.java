package net.engineerAnsh.BankApplication.exception;

public class KycNotVerifiedException extends RuntimeException {

    public KycNotVerifiedException(String message) {
        super(message);
    }
}
