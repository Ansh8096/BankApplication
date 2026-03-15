package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Account.CreateAccountDto;
import net.engineerAnsh.BankApplication.Security.UserDetails.CustomUserDetails;
import net.engineerAnsh.BankApplication.Services.AccountService;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
// This can prevent you from using @Autowired, just make the fields final (that you want to inject).
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/create-new-account") // PostMapping not PutMapping (because we are creating the resource)
    public ResponseEntity<?> createAccountForUser(@Valid @RequestBody CreateAccountDto account,
                                                  @AuthenticationPrincipal CustomUserDetails user) throws BadRequestException, JsonProcessingException {
        Long accountId = accountService.saveNewAccount(user.getUsername(), account.getAccountType());

        // this is the location where this entity is saved...
        URI locationOfAccount = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .replacePath("/api/accounts/{id}")
                .buildAndExpand(accountId)
                .toUri();

        return ResponseEntity.created(locationOfAccount).build(); // 201 Created...
    }

    @GetMapping("/get-account/{accountNumber}")
    public ResponseEntity<?> getAccountByAccountNo(@AuthenticationPrincipal CustomUserDetails user,
                                                   @PathVariable String accountNumber) throws AccessDeniedException {
        return ResponseEntity.ok().body(accountService.getTheAccountByAccountNumber(user.getUsername(), accountNumber));
    }

    @PatchMapping("/freeze/{accountNumber}")
    public ResponseEntity<?> freezeAccountByAccountNo(@AuthenticationPrincipal CustomUserDetails user,
                                                     @PathVariable String accountNumber)
            throws AccessDeniedException, JsonProcessingException {
        accountService.freezeTheAccountByAccountNumber(user.getUsername(),accountNumber);
        return ResponseEntity.ok().body("Account is successfully frozen...");
    }

    @GetMapping("/get-account-balance/{accountNumber}")
    public ResponseEntity<?> getTheAccountBalanceByAccountNumber(@AuthenticationPrincipal CustomUserDetails user,
                                                                 @PathVariable String accountNumber)
            throws AccessDeniedException {
        BigDecimal balance = accountService.getTheAccountBalanceByAccountNumber(user.getUsername(), accountNumber);
        return ResponseEntity.ok().body("Account Balance: " + balance);
    }
}
