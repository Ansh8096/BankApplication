package net.engineerAnsh.BankApplication.Controllers;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Dto.LoginRequest;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Services.UserService;
import net.engineerAnsh.BankApplication.Security.Jwt.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import javax.crypto.SecretKey;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

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

    @PutMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody User user) {
        userService.saveNewUser(user);
        return ResponseEntity.ok().build();
    }

    // It checks the user’s email & password, creates a JWT token if they are correct, and sends the token back to the client...
    @PostMapping("/login")
    public ResponseEntity<?> loginTheUser(
            @Valid @RequestBody LoginRequest request) { // @Valid → checks validations (email not empty, password not empty)...

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


}
