package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.Role;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.RoleRepository;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
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

    @Transactional
    public void saveNewUser(User user) {
        user.setKycStatus(false);
        // making sure if the role exists in the role table or not...
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
        user.getRoles().add(userRole);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        userRepository.save(user);
    }

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

    @Transactional
    public void assignRolesToTheUser(Long userId, String roleName) {
        User user = userRepository.findByUserIdAndActiveTrue(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found"));
        user.getRoles().add(role);
        userRepository.save(user);
    }

}
