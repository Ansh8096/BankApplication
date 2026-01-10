package net.engineerAnsh.BankApplication.Controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor // This can prevent you from using @Autowired, just make the fields final (that you want to inject).
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/create-new-account") // PostMapping not PutMapping (because we are creating the resource)
    public ResponseEntity<?> createAccountForUser(@RequestBody Account account) {
            Long accountId = accountService.saveNewAccount(account);

            // this is the location where this entity is saved...
            URI locationOfThisAccount = URI.create("/account/" + accountId);
            return ResponseEntity.created(locationOfThisAccount).build();
    }

    @GetMapping("/get-account/{accountId}")
    public ResponseEntity<?> getAccountById(@PathVariable Long accountId) throws AccessDeniedException {
            return ResponseEntity.ok().body(accountService.getTheAccountById(accountId));
    }

    @PutMapping("/block/{accountId}")
    public ResponseEntity<?> blockAccountById(@PathVariable Long accountId) throws AccessDeniedException {
            boolean isBloocked = accountService.blockTheAccountById(accountId);
            if (isBloocked) return ResponseEntity.ok().build();
            else return ResponseEntity.ok().body("Account is already blocked");
    }

    @PutMapping("/activate/{accountId}")
    public ResponseEntity<?> activateAccountById(@PathVariable Long accountId) throws AccessDeniedException {
            boolean isActivate = accountService.activateTheAccountById(accountId);
            if (isActivate) return ResponseEntity.ok().build();
            else return ResponseEntity.ok().body("Account is already activated");
    }

    @PutMapping("/close/{accountId}")
    public ResponseEntity<?> closeAccountById(@PathVariable Long accountId) throws AccessDeniedException {
            boolean isClosed = accountService.closeTheAccountById(accountId);
            if (isClosed) return ResponseEntity.ok().build();
            else return ResponseEntity.ok().body("Account is already closed");
    }

}
