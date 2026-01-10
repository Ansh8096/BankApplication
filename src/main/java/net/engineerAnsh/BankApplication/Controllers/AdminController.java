package net.engineerAnsh.BankApplication.Controllers;

import net.engineerAnsh.BankApplication.Dto.AssignRoleRequest;
import net.engineerAnsh.BankApplication.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/get-all-users")
    public ResponseEntity<?> getAllUsers() {
        boolean authenticated = SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
        if(!authenticated) throw new RuntimeException("This user is not an admin");
        List<?> allUsers = userService.getAllUsers();
        if (allUsers.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().body(allUsers);
    }

    @PutMapping("/assign-role/{userId}")
    public ResponseEntity<?> assignNewRoleTohTheUser(
            @PathVariable Long userId,
            @RequestBody AssignRoleRequest roleName){
        try {
            boolean authenticated = SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
            if(!authenticated) throw new RuntimeException("This user is not an admin");
            userService.assignRolesToTheUser(userId, roleName.getRoleName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
