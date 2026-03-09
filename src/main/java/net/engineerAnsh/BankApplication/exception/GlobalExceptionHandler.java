package net.engineerAnsh.BankApplication.exception;

import jakarta.mail.MessagingException;
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
public class GlobalExceptionHandler { // this helps us to avoid writing the try-catch blocks in the controllers...

    // Handling Access Denied (403)...
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        // Service throws AccessDeniedException, Controller does NOT catch it, This handler catches it (Client receives 403 Forbidden)...
        String message = null;
        if (e.getMessage() == null || e.getMessage().isEmpty()) {
            message = "You do not have permission to access this resource";
        } else message = e.getMessage();
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(message);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        return ResponseEntity
                .badRequest()
                .body(e.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUserNameNotFoundException(UsernameNotFoundException e) {
        return ResponseEntity
                .badRequest()
                .body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
                .internalServerError()
                .body(e.getMessage());
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<?> handleMessagingException(MessagingException e) {
        return ResponseEntity
                .internalServerError()
                .body(e.getMessage());
    }

    // Handle DTO validation errors (@Valid)...
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        // Field errors (like @NotNull, @Email, etc.)
        ex.getBindingResult().getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        // Global errors (like @PasswordMatches)
        ex.getBindingResult().getGlobalErrors()
                .forEach(error ->
                        errors.put(error.getObjectName(), error.getDefaultMessage())
                );


        ErrorResponse response = new ErrorResponse(
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                errors,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex) {

        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                null,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(
            TokenExpiredException ex) {

        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.GONE.value(),
                null,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.GONE);
    }

    @ExceptionHandler(KycNotVerifiedException.class)
    public ResponseEntity<?> handleKycException(KycNotVerifiedException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
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
