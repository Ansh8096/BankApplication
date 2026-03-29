package net.engineerAnsh.BankApplication.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Dto.Auth.LoginRequest;
import net.engineerAnsh.BankApplication.Dto.Auth.SignupRequest;
import net.engineerAnsh.BankApplication.Entity.EmailVerificationToken;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Entity.Role;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.KycStatus;
import net.engineerAnsh.BankApplication.Enum.OutboxEventType;
import net.engineerAnsh.BankApplication.Kafka.Builder.UserLoginEventBuilder;
import net.engineerAnsh.BankApplication.Kafka.Builder.UserRegisteredEventBuilder;
import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.UserRegisteredEvent;
import net.engineerAnsh.BankApplication.Repository.EmailVerificationTokenRepository;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
import net.engineerAnsh.BankApplication.Security.Jwt.JwtUtils;
import net.engineerAnsh.BankApplication.exception.InvalidTokenException;
import net.engineerAnsh.BankApplication.exception.TokenExpiredException;
import net.engineerAnsh.BankApplication.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserLoginEventBuilder userLoginEventBuilder;
    private final UserRegisteredEventBuilder userRegisteredEventBuilder;
    private final OutboxEventService outboxEventService;
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.verification.cooldown-minutes}")
    private int cooldownMinutes;

    @Value("${app.verification.max-resend-per-hour}")
    private int maxResendPerHour;

    @Value("${app.verification.resend-window-hours}")
    private int resendWindowHours;

    @Value("${auth.lock-duration-minutes}")
    private int authLoginLockDuration;

    @Value("${auth.login.max-attempts}")
    private int authLoginMaxAttempts;

    private void buildAndSaveOutboxEvent(Object event, OutboxEventType eventType) throws JsonProcessingException {
        OutboxEvent outboxKycEvent = outboxEventService.buildOutboxEvent(event, eventType);
        outboxEventService.publishOutBoxEvent(outboxKycEvent);
    }

    private void resetFailedAttempts(User user, String ip, String userAgent){
        user.setFailedAttempts(0);
        user.setLockTime(null);
        user.setLastLoginIp(ip);
        user.setLastLoginDevice(userAgent);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastFailedAttempt(null);
    }

    private User createNewUser(SignupRequest request){

        // making sure if the role exists in the role table or not...
        Role userRole = userService.findRoleByName("ROLE_USER"); // this will return Role if present, else Throw an exception...

        User newUser = new User();
        newUser.setName(request.getFullName());
        newUser.getRoles().add(userRole); // setting "ROLE_USER"...
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setAge(request.getAge());
        newUser.setKycStatus(KycStatus.NOT_SUBMITTED);
        newUser.setPhoneNumber(request.getPhone());
        newUser.setEmailVerified(false);
        newUser.setAccountLocked(false);
        newUser.setFailedAttempts(0);

        return newUser;
    }

    private EmailVerificationToken generateToken(User user, String tokenValue){
        return EmailVerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
    }

    private User findUser(String email){
        // If two resend requests hit at the exact same millisecond, we prevent both from generating tokens by using 'findByEmailForUpdate()' query...
        return userRepository.findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new InvalidTokenException("User not found"));
    }

    private void validateUser(User user, String email){
        if (!user.getEmail().equals(email)) {
            throw new IllegalStateException("Email doesn't belong to the user");
        }
        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public String performLogin(LoginRequest request, String ip, String userAgent) throws JsonProcessingException {

        String email = request.getEmail();

        // Try to fetch user (it may be null)...
        User user = userRepository.findByEmailAndActiveTrue(email).orElse(null);

        // If user exists, check account lock...
        if (user != null && user.isAccountLocked()) {

            // Auto-unlock after 30 minutes...
            if (user.getLockTime() != null &&
                    user.getLockTime()
                            .plusMinutes(authLoginLockDuration)
                            .isBefore(LocalDateTime.now())) {
                user.setAccountLocked(false);
                user.setFailedAttempts(0);
                user.setLockTime(null);
            } else {
                throw new LockedException("Account is locked. Try later.");
            }
        }

        // If user exists but email not verified
        if (user != null && !user.isEmailVerified()) {
            throw new DisabledException("Please verify your email first.");
        }

        Authentication authenticatedUser;
        try {
            // Authenticate password
            // A login token is created:
            // authenticationManager.authenticate(...):-
            // Calls your UserDetailsService, Loads user from database using email, Gets stored hashed password, Uses PasswordEncoder to compare passwords
            authenticatedUser = authenticationManager.authenticate( // If email & password are correct → authenticatedUser is returned, else exception thrown...
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

        } catch (AuthenticationException ex) {

            log.error("Authentication failed with exception: {}", ex.getClass().getSimpleName());

            // Only update failed attempts if user exists
            if (user != null) {

                // increment failed attempts...
                user.setFailedAttempts(user.getFailedAttempts() + 1);
                user.setLastFailedAttempt(LocalDateTime.now());

                log.info("Failed attempts for {} is: {}", user.getEmail(), user.getFailedAttempts());

                if (user.getFailedAttempts() >= authLoginMaxAttempts) {
                    user.setAccountLocked(true);
                    user.setLockTime(LocalDateTime.now());
                }

                userRepository.save(user);
            }
            throw new BadCredentialsException("Invalid credentials");
        }

        if (user == null) throw new BadCredentialsException("Invalid credentials"); // very rare...

        // Reset failed attempts after successful login...
        resetFailedAttempts(user,ip,userAgent);

        // Extract user roles:
        List<String> roles = authenticatedUser.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Creating the login event...
        UserLoginEvent loginEvent = userLoginEventBuilder.buildLoginEvent(user.getName(), request.getEmail(), ip, userAgent);

        // Saving Outbox Event...
        buildAndSaveOutboxEvent(loginEvent, OutboxEventType.USER_LOGIN);

        // Generate JWT token:
        return jwtUtils.generateToken(request.getEmail(), roles);
    }

    @Transactional
    public void saveNewUser(SignupRequest request) throws JsonProcessingException {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Creating new user...
        User newUser = createNewUser(request);

        // Saving user in DB...
        userRepository.save(newUser); // Save disabled user...

        // Generate Verification Token...
        String tokenValue = UUID.randomUUID().toString();
        EmailVerificationToken token = generateToken(newUser,tokenValue);
        tokenRepository.save(token);

        // Creating Signup Event...
        UserRegisteredEvent registrationEvent = userRegisteredEventBuilder.buildRegistrationEvent(request.getEmail(), tokenValue);

        // Saving Outbox Event...
        buildAndSaveOutboxEvent(registrationEvent,OutboxEventType.USER_REGISTERED);
    }

    @Transactional
    public void verifyEmail(String tokenValue) {

        EmailVerificationToken token = tokenRepository
                .findByToken(tokenValue)
                .orElseThrow(() ->
                        new InvalidTokenException("Invalid verification token")
                );

        // Check if already used
        if (token.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }

        // Check expiry
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token expired");
        }
        User user = token.getUser();

        // Activate user
        user.setActive(true);
        user.setEmailVerified(true);

        // mark Token as used.
        token.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(token);
    }

    @Transactional
    public void resendVerification(String email) throws JsonProcessingException {

        // If two resend requests hit at the exact same millisecond, we prevent both from generating tokens by using 'findByEmailForUpdate()' query...
        User user = findUser(email);

        // Apply required validations...
        validateUser(user,email);

        // CoolDown Check : It will limit how often a user can request verification email...
        Optional<EmailVerificationToken> latestTokenOpt = tokenRepository
                .findTopByUserOrderByCreatedAtDesc(user);

        if (latestTokenOpt.isPresent()) {
            EmailVerificationToken latestToken = latestTokenOpt.get();
            // CoolDown of 2 mins...
            if (latestToken.getCreatedAt().plusMinutes(cooldownMinutes)
                    .isAfter(LocalDateTime.now())) {
                throw new TooManyRequestsException("Please wait before requesting another verification email.");
            }
        }

        // Invalidate all the old unused tokens of a user...
        tokenRepository.findByUserAndUsedFalse(user)
                .forEach(token -> token.setUsed(true));

        // Max 5 resend emails per 1 hour. After that → block for 1 hour...
        LocalDateTime windowStart =
                LocalDateTime.now().minusHours(resendWindowHours); // 'resendWindowHours' hrs before current time...
        long resendCount =
                tokenRepository.countByUserAndCreatedAtAfter(user, windowStart); // total no of resend tokens generated for user in 'resendWindowHours' hrs...

        if (resendCount >= maxResendPerHour) {
            throw new TooManyRequestsException("Hourly resend limit reached"); // throw error if resend tokens is >= to 'maxResendPerHour'...
        }

        // Generate new token...
        String newTokenValue = UUID.randomUUID().toString();
        EmailVerificationToken newToken = generateToken(user,newTokenValue);
        tokenRepository.save(newToken);

        // Creating Signup Event...
        UserRegisteredEvent registrationEvent = userRegisteredEventBuilder.buildRegistrationEvent(email, newTokenValue);

        // Save outBox event...
        buildAndSaveOutboxEvent(registrationEvent,OutboxEventType.USER_REGISTERED);
    }
}


// Because of 'findByEmailForUpdate()' :-
//  Request A → locks user row
//  Request B → waits
//  Request A → generates token
//  Request A → commits
//  Request B → resumes
//  Request B → now cooldown check fails
//  Request B → exits


// Rate limiting means ; Max 5 requests per minute per IP
// If someone scripts:
//
// POST /resend-verification 100 times
//
// After 5 requests per minute:
//
// → HTTP 429 returned.
//
// Bot stopped.