package net.engineerAnsh.BankApplication.Controllers.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.services.account.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')") // hasRole("ADMIN") → ROLE_ADMIN → MATCH ...
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Account APIs", description = "Administrative operations for accounts")
public class AdminAccountController { // Only users that have role as: "ROLE_ADMIN, will be able to access these end points"

    private final AccountService accountService;

    @Operation(summary = "Activate account", description = "Activate account by account number")
    @PatchMapping("/account/activate")
    public ResponseEntity<?> activateAccountByAccountNo(
            @Parameter(description = "Account number") @RequestParam String accountNumber)
            throws AccessDeniedException, JsonProcessingException {
        accountService.activateTheAccount(accountNumber);
        return ResponseEntity.ok().body("Account is successfully activated...");
    }

    @Operation(summary = "Block account", description = "Block account by account number")
    @PatchMapping("/account/block")
    public ResponseEntity<?> blockAccountByAccountNo(
            @Parameter(description = "Account number") @RequestParam String accountNumber)
            throws AccessDeniedException, JsonProcessingException {
        accountService.blockTheAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok().body("Account is successfully blocked...");
    }

    @Operation(summary = "Close account", description = "Close account by account number")
    @PutMapping("/account/close")
    public ResponseEntity<?> closeAccountByAccountNo(
            @Parameter(description = "Account number") @RequestParam String accountNumber)
            throws AccessDeniedException, JsonProcessingException {
        accountService.closeTheAccount(accountNumber);
        return ResponseEntity.ok().body("Account is successfully closed...");
    }

}