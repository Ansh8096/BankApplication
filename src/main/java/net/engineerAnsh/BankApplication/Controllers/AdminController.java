package net.engineerAnsh.BankApplication.Controllers;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.PublicDto.AssignRoleRequest;
import net.engineerAnsh.BankApplication.Email.EmailServiceimpl;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Schedular.MonthlyStatementSchedular;
import net.engineerAnsh.BankApplication.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final MonthlyStatementSchedular scheduler;
    private final EmailServiceimpl emailService;

    @GetMapping("/get-all-users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> allUsers = userService.getAllUsers();
        if (allUsers.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().body(allUsers);
    }

    @PostMapping("/assign-role/{userEmail}")
    public ResponseEntity<?> assignNewRoleTohTheUser(
            @PathVariable String userEmail,
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
        scheduler.sendMonthlyStatementsManually(yearMonth);

        // Since sendMonthlyStatementsManually() is marked @Async,
        // the API will respond immediately and the job runs in background....
        return ResponseEntity.ok(
                Map.of(
                        "message", "Monthly statements triggered successfully in background",
                        "month", yearMonth.toString()
                )
        );
    }


}
