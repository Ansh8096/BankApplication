package net.engineerAnsh.BankApplication.Controllers.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.services.statement.AdminStatementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.Map;

@PreAuthorize("hasRole('ADMIN')") // hasRole("ADMIN") → ROLE_ADMIN → MATCH ...
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Statement APIs", description = "Administrative operations for account statements")
public class AdminStatementController { // Only users that have role as: "ROLE_ADMIN, will be able to access these end points"

    private final AdminStatementService adminStatementService;

    @Operation(summary = "Trigger monthly statement", description = "Send monthly statements asynchronously")
    @PostMapping("/monthly-statement/send")
    public ResponseEntity<?> runJob(
            @Parameter(description = "Month in yyyy-MM format", example = "2026-01")
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

}