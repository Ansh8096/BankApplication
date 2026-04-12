package net.engineerAnsh.BankApplication.Controllers;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Account.AccountResponse;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.services.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<String> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(auth.getName());
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<?> deleteUser() {
        userService.softDeleteTheUserByEmail();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update-user")
    public ResponseEntity<?> updateUser(@RequestBody User user) {
        userService.updateTheUser(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/get-user")
    public ResponseEntity<User> getUserByEmail() {
        User user = userService.getTheUserByEmail();
        return ResponseEntity.ok().body(user);
    }

    @GetMapping("/get-all-accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccountsOfTheUser() {
        List<AccountResponse> allAccountsOfUser = userService.getAllAccountsOfUser();
        return ResponseEntity.ok().body(allAccountsOfUser);
    }

}