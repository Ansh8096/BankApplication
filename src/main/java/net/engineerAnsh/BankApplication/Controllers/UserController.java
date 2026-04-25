package net.engineerAnsh.BankApplication.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Account.AccountResponse;
import net.engineerAnsh.BankApplication.Dto.user.UpdatePasswordRequest;
import net.engineerAnsh.BankApplication.Dto.user.UpdateUserRequest;
import net.engineerAnsh.BankApplication.Dto.user.UserResponse;
import net.engineerAnsh.BankApplication.services.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User APIs", description = "Operations related to user profile and account details")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Update user", description = "Update user profile details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/update-user")
    public ResponseEntity<String> updateUser(
            @Parameter(description = "User update request payload", required = true)
            @Valid @RequestBody UpdateUserRequest request)
    {
        userService.updateTheUser(request);
        return ResponseEntity.ok("User is updated successfully...");
    }

    @Operation(summary = "Update password", description = "Update user password securely")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(
            @Parameter(description = "Password update request payload", required = true)
            @Valid @RequestBody UpdatePasswordRequest request)
    {

        userService.updatePassword(request);
        return ResponseEntity.ok("Password updated successfully");
    }


    @Operation(summary = "Get user details", description = "Fetch logged-in user's profile details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User details fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/get-user")
    public ResponseEntity<UserResponse> getUserByEmail() {
        UserResponse userByEmail = userService.getTheUserByEmail();
        return ResponseEntity.ok().body(userByEmail);
    }

    @Operation(summary = "Get all user accounts", description = "Fetch all bank accounts of the logged-in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/get-all-accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccountsOfTheUser() {
        List<AccountResponse> allAccountsOfUser = userService.getAllAccountsOfUser();
        return ResponseEntity.ok().body(allAccountsOfUser);
    }

}