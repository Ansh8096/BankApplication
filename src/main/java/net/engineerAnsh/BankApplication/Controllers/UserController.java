package net.engineerAnsh.BankApplication.Controllers;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// it will also contain all the transaction Api's,getting account Statement,

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @DeleteMapping("/delete-user")
    public ResponseEntity<?> deleteUser() {
        userService.softDeleteTheUserByEmail();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-user")
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
    public ResponseEntity<List<Account>> getAllAccountsOfTheUser() {
        List<Account> allAccountsOfUser = userService.getAllAccountsOfUser();
        return ResponseEntity.ok().body(allAccountsOfUser);
    }

}