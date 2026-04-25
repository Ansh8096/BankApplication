package net.engineerAnsh.BankApplication.exception.exceptions;

public class PhoneAlreadyUsedException extends RuntimeException {
    public PhoneAlreadyUsedException(String message) {
        super(message);
    }
}
