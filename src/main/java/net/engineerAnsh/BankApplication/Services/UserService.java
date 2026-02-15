package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.PublicDto.SignupRequest;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.Role;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.RoleRepository;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String getUserEmail() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    public User findUser(String email) {
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("This User is not found"));
    }

    public Role findRoleByName(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("This role doesn't exists"));
    }

    @Transactional
    public void saveNewUser(SignupRequest request) {

        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("Email already registered");
        }
        // making sure if the role exists in the role table or not...
        Role userRole = findRoleByName("ROLE_USER"); // this will return Role if present, else Throw an exception...

        // Creating new user...
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.getRoles().add(userRole); // setting "ROLE_USER"...
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setAge(request.getAge());
        newUser.setKycStatus(false);
        newUser.setPhoneNumber(request.getPhone());

        // Saving user in DB...
        userRepository.save(newUser);
    }

    @PreAuthorize("hasRole('ADMIN')") // Even if someone bypasses the controller, service is protected...
    public List<User> getAllUsers() {
        return userRepository.findAllActiveUsers();
    }

    @Transactional
    public void softDeleteTheUserByEmail() {
        String email = getUserEmail();
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
    public void updateTheUser(User user) {
        String email = getUserEmail();
        User savedUser = findUser(email);

        if (user.getName() != null && !user.getName().isEmpty()) {
            savedUser.setName(user.getName());
        }

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            savedUser.setEmail(user.getEmail());
        }

        if (user.getAge() != null && user.getAge() != 0) {
            savedUser.setAge(user.getAge());
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            savedUser.setPhoneNumber(user.getPhoneNumber());
        }

        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            savedUser.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        userRepository.save(savedUser);
    }

    public User getTheUserByEmail() {
        String email = getUserEmail();
        return findUser(email);
    }

    public List<Account> getAllAccountsOfUser() {
        String email = getUserEmail();
        return accountRepository.findByUserEmail(email); // it returns : List<Account>
    }

    @PreAuthorize("hasRole('ADMIN')") // Even if someone bypasses the controller, service is protected...
    @Transactional
    public void assignRolesToTheUser(String userEmail, String roleName) {
        User user = findUser(userEmail);
        Role role = findRoleByName(roleName); // this will return Role if present, else Throw an exception...
        user.getRoles().add(role);
        userRepository.save(user);
    }

}
