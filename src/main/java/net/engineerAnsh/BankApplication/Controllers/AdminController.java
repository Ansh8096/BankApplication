package net.engineerAnsh.BankApplication.Controllers;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.AssignRoleRequest;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController { // Only users that have role as: "ROLE_ADMIN, will be able to access these end points"

    private final UserService userService;

    @GetMapping("/get-all-users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> allUsers = userService.getAllUsers();
        if (allUsers.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().body(allUsers);
    }

    @PostMapping("/assign-role/{userId}")
    public ResponseEntity<?> assignNewRoleTohTheUser(
            @PathVariable Long userId,
            @RequestBody AssignRoleRequest roleName)
    {
            userService.assignRolesToTheUser(userId, roleName.getRoleName());
            return ResponseEntity.ok().build();
    }
}
