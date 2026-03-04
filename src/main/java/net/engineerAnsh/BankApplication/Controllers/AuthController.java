package net.engineerAnsh.BankApplication.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Auth.LoginRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.ResendVerificationRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.SignupRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.SignupResponse;
import net.engineerAnsh.BankApplication.Services.AuthService;
import net.engineerAnsh.BankApplication.Services.RedisRateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RedisRateLimiterService redisRateLimiterService;

    @GetMapping("/health-check")
    public String applicationStatus() {
        return "ok";
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(
            @Valid @RequestBody SignupRequest request) { // @Valid → checks validations (email not empty, password not empty)...
        authService.saveNewUser(request);
        URI locationOfUser = URI.create("/account/" + request.getEmail());
        return ResponseEntity.created(locationOfUser).body(new SignupResponse("Registration successful. Please verify your email."));
    }

    // It checks the user’s email & password, creates a JWT token if they are correct, and sends the token back to the client...
    @PostMapping("/login")
    public ResponseEntity<?> loginTheUser(
            // @Valid → checks validations (email not empty, password not empty)...
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        // Rate limiting...
        String email = request.getEmail();
        String ip = httpRequest.getHeader("X-Forwarded-For"); // best after deployment...

        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = httpRequest.getRemoteAddr();
        }

        // Apply Rate Limiting...
        boolean ipAllowed = redisRateLimiterService.isAllowed("login:ip:" + ip);
        boolean emailAllowed = redisRateLimiterService.isAllowed("login:email:" + email);

        if (!ipAllowed || !emailAllowed) {
            return ResponseEntity.status(429)
                    .body("Too many login attempts. Please try again later.");
        }

        // Extracting the user agent...
        String userAgent = httpRequest.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "Unknown";
        }

        String jwtToken = authService.performLogin(request, ip, userAgent);

        return ResponseEntity.ok(
                Map.of(
                        "accessToken", jwtToken,
                        "tokenType", "Bearer"
                )
        );
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(
                "Email verified successfully. You can now login."
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {

        String email = request.getEmail();
        String ip = httpRequest.getHeader("X-Forwarded-For"); // best after deployment...

        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = httpRequest.getRemoteAddr();
        }

        // Rate Limiting...
        // We are going for the Dual protection: (Because attacker can: Rotate IP or Target single email)
        // Single IP can be spamming many emails...
        // Single IP spamming many emails...
        boolean ipAllowed = redisRateLimiterService.isAllowed("signup:ip:" + ip);
        boolean emailAllowed = redisRateLimiterService.isAllowed("signup:email:" + email);

        if (!ipAllowed || !emailAllowed) {
            return ResponseEntity.status(429)
                    .body("Too many requests. Try again later.");
        }

        authService.resendVerification(email);
        return ResponseEntity.ok(
                "If your email is not verified, a verification link has been sent."
        );
    }

}
