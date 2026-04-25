package net.engineerAnsh.BankApplication.exception.exceptionHandler;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.exception.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice -> Think of it as: “A global try–catch for all controllers”...
// It Listens for exceptions thrown anywhere (such as from services, controllers etc.) & Converts them to custom HTTP responses, helps to keep the controllers clean...
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler { // this helps us to avoid writing the try-catch blocks in the controllers...


    // Common builder method (DRY)
    private ResponseEntity<ErrorResponse> buildError(String message, HttpStatus status) {
        ErrorResponse error = new ErrorResponse(
                message,
                status.value(),
                null,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, status);
    }

    // ===============
    // AUTH / ACCESS
    // ===============

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildError(
                ex.getMessage() != null ? ex.getMessage() : "Access denied",
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UsernameNotFoundException ex) {
        return buildError(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // ===============
    // EMAIL / TOKEN
    // ===============
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException ex) {
        return buildError(ex.getMessage(), HttpStatus.GONE);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return buildError(ex.getMessage(), HttpStatus.CONFLICT);
    }

    // =================
    // PHONE / OTP
    // =================

    @ExceptionHandler(PhoneAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handlePhoneAlreadyUsed(PhoneAlreadyUsedException ex) {
        return buildError(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AlreadyVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyVerified(AlreadyVerifiedException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(InvalidOtpException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpExpired(OtpExpiredException ex) {
        return buildError(ex.getMessage(), HttpStatus.GONE);
    }

    @ExceptionHandler(OtpBlockedException.class)
    public ResponseEntity<ErrorResponse> handleOtpBlocked(OtpBlockedException ex) {
        return buildError(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

    // =============
    // PASSWORD
    // =============

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SamePasswordException.class)
    public ResponseEntity<ErrorResponse> handleSamePassword(SamePasswordException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // ========================
    // BUSINESS (KYC / FRAUD)
    // ========================

    @ExceptionHandler(KycNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleKyc(KycNotVerifiedException ex) {
        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(FraudDetectedException.class)
    public ResponseEntity<ErrorResponse> handleFraud(FraudDetectedException ex) {
        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    // ==============
    // VALIDATION
    // ==============

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.put(error.getObjectName(), error.getDefaultMessage()));

        ErrorResponse response = new ErrorResponse(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                errors,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ================
    // CLIENT ERRORS
    // ================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // =======
    // MAIL
    // =======

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ErrorResponse> handleMail(MessagingException ex) {
        log.error("Email sending failed", ex);
        return buildError("Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===================================
    // GLOBAL FALLBACK (VERY IMPORTANT)
    // ===================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobal(Exception ex) {

        log.error("Unhandled exception occurred", ex);

        return buildError(
                "Something went wrong. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}


// Now if signup fails validation, response becomes:
//      {
//        "message": "Validation Failed",
//        "status": 400,
//        "timestamp": "2026-02-28T12:10:33",
//        "errors": {
//          "email": "Invalid email format",
//          "age": "You must be at least 18 years old"
//        }
//      }
