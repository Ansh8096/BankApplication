package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Account.AccountResponse;
import net.engineerAnsh.BankApplication.Dto.Account.CreateAccountDto;
import net.engineerAnsh.BankApplication.Security.UserDetails.CustomUserDetails;
import net.engineerAnsh.BankApplication.services.account.AccountService;
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
@RequiredArgsConstructor// This can prevent you from using @Autowired, just make the fields final (that you want to inject).
@Tag(name = "Account APIs", description = "Operations related to bank account management")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Create new account", description = "Create a new bank account for authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/create-new-account") // PostMapping not PutMapping (because we are creating the resource)
    public ResponseEntity<?> createAccountForUser(
            @Parameter(description = "Account creation request payload", required = true)
            @Valid @RequestBody CreateAccountDto account,

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails user
    ) throws BadRequestException, JsonProcessingException {

        Long accountId = accountService.saveNewAccount(user.getUsername(), account.getAccountType());

        // this is the location where this entity is saved...
        URI locationOfAccount = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .replacePath("/api/accounts/{id}")
                .buildAndExpand(accountId)
                .toUri();

        return ResponseEntity.created(locationOfAccount).build(); // 201 Created...
    }

    @Operation(summary = "Get account details", description = "Fetch account details using account number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account fetched successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/get-account/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNo(

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails user,

            @Parameter(description = "Account number", example = "1234567890")
            @PathVariable String accountNumber
    ) throws AccessDeniedException {

        return ResponseEntity.ok().body(
                accountService.getTheAccountByAccountNumber(user.getUsername(), accountNumber)
        );
    }

    @Operation(summary = "Freeze account", description = "Freeze account by account number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account frozen successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PatchMapping("/freeze/{accountNumber}")
    public ResponseEntity<?> freezeAccountByAccountNo(

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails user,

            @Parameter(description = "Account number", example = "1234567890")
            @PathVariable String accountNumber
    )
            throws AccessDeniedException, JsonProcessingException {

        accountService.freezeTheAccountByAccountNumber(user.getUsername(), accountNumber);
        return ResponseEntity.ok().body("Account is successfully frozen...");
    }

    @Operation(summary = "Get account balance", description = "Fetch account balance by account number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance fetched successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/get-account-balance/{accountNumber}")
    public ResponseEntity<?> getTheAccountBalanceByAccountNumber(

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails user,

            @Parameter(description = "Account number", example = "1234567890")
            @PathVariable String accountNumber
    ) throws AccessDeniedException
    {

        BigDecimal balance = accountService.getTheAccountBalanceByAccountNumber(user.getUsername(), accountNumber);
        return ResponseEntity.ok().body("Account Balance: " + balance);
    }
}
