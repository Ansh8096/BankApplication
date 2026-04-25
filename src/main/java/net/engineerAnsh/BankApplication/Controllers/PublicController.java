package net.engineerAnsh.BankApplication.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.services.user.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public")
public class PublicController {

    private final UserService userService;

    @Operation(summary = "Health Check", description = "Check if application is running")
    @GetMapping("/health-check")
    public String applicationStatus() {
        return "ok";
    }

}
