package net.engineerAnsh.BankApplication.Controllers.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Auth.AssignRoleRequest;
import net.engineerAnsh.BankApplication.Dto.user.AdminUserResponse;
import net.engineerAnsh.BankApplication.services.notification.email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.services.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')") // hasRole("ADMIN") → ROLE_ADMIN → MATCH ...
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin User APIs", description = "Administrative operations for users")
public class AdminUserController { // Only users that have role as: "ROLE_ADMIN, will be able to access these end points"

    private final UserService userService;
    private final EmailServiceImpl emailService;

    @Operation(summary = "Get all users", description = "Fetch all registered users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users fetched successfully"),
            @ApiResponse(responseCode = "204", description = "No users found")
    })
    @GetMapping("/get-all-users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> allUsers = userService.getAllUsers();
        if (allUsers.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().body(allUsers);
    }


    @Operation(summary = "Delete user", description = "Soft delete a user by email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully")
    })
    @DeleteMapping("/delete-user")
    public ResponseEntity<?> deleteUser(
            @Parameter(description = "User email", example = "user@gmail.com")
            @RequestParam String userEmail)
    {
        userService.softDeleteTheUserByEmail(userEmail);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Assign role", description = "Assign a new role to a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role assigned successfully")
    })
    @PostMapping("/assign-role")
    public ResponseEntity<?> assignNewRoleTohTheUser(
            @Parameter(description = "User email", example = "user@gmail.com")
            @RequestParam String userEmail,

            @Parameter(description = "Role request body")
            @RequestBody AssignRoleRequest roleName)
    {
        userService.assignRolesToTheUser(userEmail, roleName.getRoleName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Send email", description = "Send a manual email")
    @PostMapping("/email/send")
    public ResponseEntity<?> sendEmail(
            @Parameter(description = "Recipient email") @RequestParam("to") String to,
            @Parameter(description = "Email subject") @RequestParam("subject") String subject,
            @Parameter(description = "Email body") @RequestParam("body") String body
    ) {
        emailService.sendSimpleEmail(to, subject, body);
        return ResponseEntity.ok().build();
    }

}