package net.engineerAnsh.BankApplication.Controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Account.CreateAccountDto;
import net.engineerAnsh.BankApplication.Services.AccountService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> createAccountForUser(@Valid @RequestBody CreateAccountDto account) {
        Long accountId = accountService.saveNewAccount(account.getAccountType());

        // this is the location where this entity is saved...
        URI locationOfAccount = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .replacePath("/api/accounts/{id}")
                .buildAndExpand(accountId)
                .toUri();

        return ResponseEntity.created(locationOfAccount).build(); // 201 Created...
    }

    @GetMapping("/get-account/{accountNumber}")
    public ResponseEntity<?> getAccountByAccountNo(@PathVariable String accountNumber) throws AccessDeniedException {
        return ResponseEntity.ok().body(accountService.getTheAccountByAccountNumber(accountNumber));
    }

    @PutMapping("/block/{accountNumber}")
    public ResponseEntity<?> blockAccountByAccountNo(@PathVariable String accountNumber) throws AccessDeniedException {
        boolean isBloocked = accountService.blockTheAccountByAccountNumber(accountNumber);
        if (isBloocked) return ResponseEntity.ok().build();
        else return ResponseEntity.ok().body("Account is already blocked");
    }

    @PutMapping("/activate/{accountNumber}")
    public ResponseEntity<?> activateAccountByAccountNo(@PathVariable String accountNumber) throws AccessDeniedException {
        boolean isActivate = accountService.activateTheAccountByAccountNumber(accountNumber);
        if (isActivate) return ResponseEntity.ok().build();
        else return ResponseEntity.ok().body("Account is already activated");
    }

    @PutMapping("/close/{accountNumber}")
    public ResponseEntity<?> closeAccountByAccountNo(@PathVariable String accountNumber) throws AccessDeniedException {
        boolean isClosed = accountService.closeTheAccountByAccountNumber(accountNumber);
        if (isClosed) return ResponseEntity.ok().build();
        else return ResponseEntity.ok().body("Account is already closed");
    }

    @GetMapping("/get-account-balance/{accountNumber}")
    public ResponseEntity<?> getTheAccountBalanceByAccountNumber(@PathVariable String accountNumber) throws AccessDeniedException {
        BigDecimal balance = accountService.getTheAccountBalanceByAccountNumber(accountNumber);
        return ResponseEntity.ok().body("Account Balance: " + balance);
    }

}
