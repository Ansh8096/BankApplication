package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Auth.AssignRoleRequest;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycReviewRequest;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycStatusResponse;
import net.engineerAnsh.BankApplication.services.notification.email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Security.UserDetails.CustomUserDetails;
import net.engineerAnsh.BankApplication.services.account.AccountService;
import net.engineerAnsh.BankApplication.services.statement.AdminStatementService;
import net.engineerAnsh.BankApplication.services.kyc.KycService;
import net.engineerAnsh.BankApplication.services.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@PreAuthorize("hasRole('ADMIN')") // hasRole("ADMIN") → ROLE_ADMIN → MATCH ...
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController { // Only users that have role as: "ROLE_ADMIN, will be able to access these end points"

    private final UserService userService;
    private final AdminStatementService adminStatementService;
    private final EmailServiceImpl emailService;
    private final AccountService accountService;
    private final KycService kycService;

    @GetMapping("/get-all-users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> allUsers = userService.getAllUsers();
        if (allUsers.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().body(allUsers);
    }

    @PostMapping("/assign-role")
    public ResponseEntity<?> assignNewRoleTohTheUser(
            @RequestParam String userEmail,
            @RequestBody AssignRoleRequest roleName)
    {
        userService.assignRolesToTheUser(userEmail, roleName.getRoleName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/send")
    public ResponseEntity<?> sendEmail(
            @RequestParam("to") String to,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body
    ) {
        emailService.sendSimpleEmail(to, subject, body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/monthly-statement/send")
    public ResponseEntity<?> runJob(
            @RequestParam("month") String month)
    {
        YearMonth yearMonth;
        // Checking if the given String follows YearMonth pattern or not, If not return an error...
        try {
            yearMonth = YearMonth.parse(month); // expects yyyy-MM...
        } catch (Exception e) {
            // returning the 'map' of details in a response...
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message", "Invalid month format. Use yyyy-MM. Example: 2026-01",
                            "status", 400
                    )
            );
        }
        adminStatementService.sendMonthlyStatementsManually(yearMonth);

        // Since sendMonthlyStatementsManually() is marked @Async,
        // the API will respond immediately and the job runs in background....
        return ResponseEntity.ok(
                Map.of(
                        "message", "Monthly statements triggered successfully in background",
                        "month", yearMonth.toString()
                )
        );
    }

    @PatchMapping("/account/activate")
    public ResponseEntity<?> activateAccountByAccountNo(@RequestParam String accountNumber)
            throws AccessDeniedException, JsonProcessingException {
        accountService.activateTheAccount(accountNumber);
        return ResponseEntity.ok().body("Account is successfully activated...");
    }

    @PatchMapping("/account/block")
    public ResponseEntity<?> blockAccountByAccountNo(@RequestParam String accountNumber)
            throws AccessDeniedException, JsonProcessingException {
        accountService.blockTheAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok().body("Account is successfully blocked...");
    }

    @PutMapping("/account/close")
    public ResponseEntity<?> closeAccountByAccountNo(@RequestParam String accountNumber)
            throws AccessDeniedException, JsonProcessingException {
        accountService.closeTheAccount(accountNumber);
        return ResponseEntity.ok().body("Account is successfully closed...");
    }

    @GetMapping("/api/v1/kyc/pending")
    public List<KycStatusResponse> getPendingKyc() {
        return kycService.getPendingKyc();
    }

    @PostMapping("api/v1/kyc/review")
    public ResponseEntity<String> reviewKyc(
            @RequestParam String kycId,
            @AuthenticationPrincipal CustomUserDetails admin,
            @Valid @RequestBody KycReviewRequest request) throws JsonProcessingException {

        kycService.reviewKyc(kycId, admin.getUsername(), request);
        return ResponseEntity.ok("Documents are successfully reviewed...");
    }

}