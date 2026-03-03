package net.engineerAnsh.BankApplication.Controllers;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Auth.LoginRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.ResendVerificationRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.SignupRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.SignupResponse;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Services.AuthService;
import net.engineerAnsh.BankApplication.Services.RedisRateLimiterService;
import net.engineerAnsh.BankApplication.Services.UserService;
import net.engineerAnsh.BankApplication.Security.Jwt.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import javax.crypto.SecretKey;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final RedisRateLimiterService redisRateLimiterService;


    @GetMapping("/health-check")
    public String applicationStatus() {
        return "ok";
    }

    // Generate a strong secret key (do this ONCE), then put this value in env file or application.properties...
    @GetMapping("/token")
    public String tokenInit() {
        SecretKey key = Jwts.SIG.HS256.key().build();
        return Encoders.BASE64.encode(key.getEncoded());
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
            @Valid @RequestBody LoginRequest request) {

        // A login token is created:
        // authenticationManager.authenticate(...):-
        // Calls your UserDetailsService, Loads user from database using email, Gets stored hashed password, Uses PasswordEncoder to compare passwords
        Authentication authenticatedUser = authenticationManager.authenticate( // If email & password are correct → authenticatedUser is returned, else exception thrown...
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Extract user roles:
        List<String> roles = authenticatedUser.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Generate JWT token:
        String jwtToken = jwtUtils.generateToken(request.getEmail(), roles);
        return ResponseEntity.ok().body(jwtToken);
    }

    @GetMapping("/get-all-users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> allUsers = userService.getAllUsers();
        if (allUsers.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().body(allUsers);
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
        String ip = httpRequest.getRemoteAddr();
        String email = request.getEmail();

        // We are going for the Dual protection: (Because attacker can: Rotate IP or Target single email)
        // Single IP can be spamming many emails...
        // Single IP spamming many emails...
        boolean ipAllowed = redisRateLimiterService.isAllowed("ip:" + ip);
        boolean emailAllowed = redisRateLimiterService.isAllowed("email:" + email);

        if (!ipAllowed || !emailAllowed) {
            return ResponseEntity.status(429)
                    .body("Too many requests. Try again later.");
        }

        authService.resendVerification(email);
        return ResponseEntity.ok(
                "If your email is not verified, a verification link has been sent."
        );
    }

    @GetMapping("/redis-test")
    public void redistest() {
        authService.sampleData("name", "shaily");
    }

}
