package net.engineerAnsh.BankApplication.services.user;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Account.AccountResponse;
import net.engineerAnsh.BankApplication.Dto.user.AdminUserResponse;
import net.engineerAnsh.BankApplication.Dto.user.UpdatePasswordRequest;
import net.engineerAnsh.BankApplication.Dto.user.UpdateUserRequest;
import net.engineerAnsh.BankApplication.Dto.user.UserResponse;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.Role;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.account.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.kyc.KycStatus;
import net.engineerAnsh.BankApplication.Enum.user.PhoneVerificationStatus;
import net.engineerAnsh.BankApplication.Repository.RoleRepository;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
import net.engineerAnsh.BankApplication.exception.exceptions.*;
import net.engineerAnsh.BankApplication.services.account.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AccountService accountService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserResponse mapToUserResponse(User user){
        return new UserResponse(
                user.getName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhoneNumber(),
                user.getPhoneVerificationStatus(),
                user.getAge(),
                user.getKycStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getLastLoginDevice()
        );
    }

    private AdminUserResponse mapToUserAdminResponse(User user){
        return new AdminUserResponse(
                user.getName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhoneNumber(),
                user.getPhoneVerificationStatus(),
                user.getAge(),
                user.getKycStatus(),
                user.getActive(),
                user.isAccountLocked(),
                user.getFailedAttempts(),
                user.getLockTime(),
                user.getLastFailedAttempt(),
                user.getLastLoginIp(),
                user.getLastLoginLocation(),
                user.getLastLoginDevice(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }

    private void kycCheck(KycStatus status) {
        if (status != KycStatus.APPROVED) {
            throw new KycNotVerifiedException("KYC verification required");
        }
    }

    public String getUserEmail() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    public User findUser(String email) {
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    }

    public Role findRoleByName(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("This role doesn't exists"));
    }

    @PreAuthorize("hasRole('ADMIN')") // Even if someone bypasses the controller, service is protected...
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAllActiveUsers()
                .stream()
                .map(this::mapToUserAdminResponse)
                .toList();
    }

    public User findUserByPhone(String phone) {
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')") // Even if someone bypasses the controller, service is protected...
    @Transactional
    public void softDeleteTheUserByEmail(String email) {
        User user = findUser(email);
        // soft deleting the user...
        user.setDeletedAt(LocalDateTime.now());
        user.setActive(false);
        // soft deleting the accounts of a user...
        for (Account accounts : user.getAccounts()) {
            accounts.setAccountStatus(AccountStatus.CLOSED);
            accounts.setAccountClosedAt(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    @Transactional
    public void updateTheUser(UpdateUserRequest request) {

        String email = getUserEmail();
        User savedUser = findUser(email);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            savedUser.setName(request.getName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {

            String newEmail = request.getEmail().trim().toLowerCase();

            if (!newEmail.equals(savedUser.getEmail()) &&
                    userRepository.existsByEmailAndActiveTrueAndDeletedAtIsNull(newEmail)) {

                throw new EmailAlreadyUsedException("Email already in use");
            }

            if (!newEmail.equals(savedUser.getEmail())) {
                savedUser.setEmail(newEmail);
                savedUser.setEmailVerified(false);
            }
        }

        if (request.getAge() != null) {
            savedUser.setAge(request.getAge());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {

            String newPhone = request.getPhoneNumber().trim();

            if (!newPhone.equals(savedUser.getPhoneNumber()) &&
                    userRepository.existsByPhoneNumberAndActiveTrueAndDeletedAtIsNull(newPhone)) {

                throw new PhoneAlreadyUsedException("Phone already in use");
            }

            if (!newPhone.equals(savedUser.getPhoneNumber())) {
                savedUser.setPhoneNumber(newPhone);
                savedUser.setPhoneVerificationStatus(PhoneVerificationStatus.PENDING);
            }
        }
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {

        String email = getUserEmail();
        User user = findUser(email);

        // 1. Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Old password is incorrect");
        }

        // 2. Prevent same password reuse
        if (passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new SamePasswordException("New password cannot be same as old password");
        }

        // 3. Encode new password
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        user.setPasswordHash(encodedPassword);

        // 4. security resets
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
    }

    public UserResponse getTheUserByEmail() {
        String email = getUserEmail();
        return mapToUserResponse(findUser(email));
    }

    public List<AccountResponse> getAllAccountsOfUser() {
        String email = getUserEmail();
        return accountService.getAllAccounts(email);
    }

    @PreAuthorize("hasRole('ADMIN')") // Even if someone bypasses the controller, service is protected...
    @Transactional
    public void assignRolesToTheUser(String userEmail, String roleName) {
        User user = findUser(userEmail);
        Role role = findRoleByName(roleName); // this will return Role if present, else Throw an exception...
        kycCheck(user.getKycStatus()); // kyc check...
        user.getRoles().add(role);
        userRepository.save(user);
    }

}
