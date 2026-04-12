package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Auth.LoginRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.ResendVerificationRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.SignupRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.SignupResponse;
import net.engineerAnsh.BankApplication.Dto.Otp.SendOtpRequest;
import net.engineerAnsh.BankApplication.Dto.Otp.VerifyOtpRequest;
import net.engineerAnsh.BankApplication.services.auth.AuthService;
import net.engineerAnsh.BankApplication.services.auth.OtpService;
import net.engineerAnsh.BankApplication.services.auth.RedisRateLimiterService;
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
    private final OtpService otpService;

    @GetMapping("/health-check")
    public String applicationStatus() {
        return "ok";
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(
            @Valid @RequestBody SignupRequest request) throws JsonProcessingException { // @Valid → checks validations (email not empty, password not empty)...
        authService.saveNewUser(request);
        URI locationOfUser = URI.create("/account/" + request.getEmail());
        return ResponseEntity.created(locationOfUser).body(new SignupResponse("Registration successful. Please verify your email."));
    }

    // It checks the user’s email & password, creates a JWT token if they are correct, and sends the token back to the client...
    @PostMapping("/login")
    public ResponseEntity<?> loginTheUser(
            // @Valid → checks validations (email not empty, password not empty)...
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) throws JsonProcessingException {

        // Rate limiting...
        String email = request.getEmail();
        String ip = httpRequest.getHeader("X-Forwarded-For");

        String unknown = "unknown";
        if (ip != null && !ip.isEmpty() && !unknown.equalsIgnoreCase(ip)) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = httpRequest.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpRequest.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpRequest.getRemoteAddr();
        }

        // Handle IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
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
    public ResponseEntity<?> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) throws JsonProcessingException {

        String email = request.getEmail();
        String ip = httpRequest.getHeader("X-Forwarded-For");

        String unknown = "unknown";
        if (ip != null && !ip.isEmpty() && !unknown.equalsIgnoreCase(ip)) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = httpRequest.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpRequest.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || unknown.equalsIgnoreCase(ip)) {
            ip = httpRequest.getRemoteAddr();
        }

        // Handle IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
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

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody SendOtpRequest request) throws JsonProcessingException {

        otpService.generateAndSendOtp(request.getPhoneNumber());

        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody VerifyOtpRequest request) {

        otpService.verifyOtpAndActivateUser(
                request.getPhoneNumber(),
                request.getOtp()
        );

        return ResponseEntity.ok("Phone number verified successfully");
    }

}
