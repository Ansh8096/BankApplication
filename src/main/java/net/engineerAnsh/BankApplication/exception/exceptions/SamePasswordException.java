package net.engineerAnsh.BankApplication.exception.exceptions;

public class SamePasswordException extends RuntimeException {
    public SamePasswordException(String message) {
        super(message);
    }
}
