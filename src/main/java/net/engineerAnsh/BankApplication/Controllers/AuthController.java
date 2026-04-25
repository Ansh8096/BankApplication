package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth APIs", description = "Authentication, login, signup, email verification and OTP operations")
public class AuthController {

    private final AuthService authService;
    private final RedisRateLimiterService redisRateLimiterService;
    private final OtpService otpService;

    @Operation(summary = "User Signup", description = "Register a new user account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(
            @Parameter(description = "Signup request payload", required = true)
            @Valid @RequestBody SignupRequest request) throws JsonProcessingException { // @Valid → checks validations (email not empty, password not empty)...
        authService.saveNewUser(request);
        URI locationOfUser = URI.create("/account/" + request.getEmail());
        return ResponseEntity.created(locationOfUser).body(new SignupResponse("Registration successful. Please verify your email."));
    }

    // It checks the user’s email & password, creates a JWT token if they are correct, and sends the token back to the client...
    @Operation(summary = "User Login", description = "Authenticate user and return JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "429", description = "Too many login attempts"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> loginTheUser(
            // @Valid → checks validations (email not empty, password not empty)...
            @Parameter(description = "Login request payload", required = true)
            @Valid @RequestBody LoginRequest request,
            @Parameter(hidden = true)
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

    @Operation(summary = "Verify Email", description = "Verify user email using token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(
            @Parameter(description = "Email verification token")
            @RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(
                "Email verified successfully. You can now login."
        );
    }

    @Operation(summary = "Resend Verification Email", description = "Resend email verification link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification email sent"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(
            @Parameter(description = "Email request payload", required = true)
            @Valid @RequestBody ResendVerificationRequest request,
            @Parameter(hidden = true)
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

    @Operation(summary = "Send OTP", description = "Generate and send OTP to user's phone number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(
            @Parameter(description = "Phone number request payload")
            @RequestBody SendOtpRequest request) throws JsonProcessingException {
        otpService.generateAndSendOtp(request.getPhoneNumber());
        return ResponseEntity.ok("OTP sent successfully");
    }

    @Operation(summary = "Verify OTP", description = "Verify OTP and activate user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP"),
            @ApiResponse(responseCode = "429", description = "Too many attempts")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(
            @Parameter(description = "OTP verification request payload")
            @RequestBody VerifyOtpRequest request)
    {
        otpService.verifyOtpAndActivateUser(
                request.getPhoneNumber(),
                request.getOtp()
        );
        return ResponseEntity.ok("Phone number verified successfully");
    }

}
