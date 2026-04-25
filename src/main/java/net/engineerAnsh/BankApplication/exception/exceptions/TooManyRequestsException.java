package net.engineerAnsh.BankApplication.exception.exceptions;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }

}