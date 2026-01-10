package net.engineerAnsh.BankApplication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

// @RestControllerAdvice -> Think of it as: “A global try–catch for all controllers”...
// It Listens for exceptions thrown anywhere (such as services, controllers etc.) & Converts them to HTTP responses, helps to keep the controllers clean...
@RestControllerAdvice
public class GlobalExceptionHandler { // this helps us to avoid writing the try-catch blocks in the controllers...

    // Handling Access Denied (403)...
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e){
        // Service throws AccessDeniedException...
        // Controller does NOT catch it, This handler catches it (Client receives 403 Forbidden)...
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e){
        // Service throws RuntimeException...
        // Controller does NOT catch it, This handler catches it (Client receives 400 Forbidden)...
        return ResponseEntity
                .badRequest()
                .body(e.getMessage());
    }
}
