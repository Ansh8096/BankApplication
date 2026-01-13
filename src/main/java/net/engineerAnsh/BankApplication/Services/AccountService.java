package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.AccountType;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
import net.engineerAnsh.BankApplication.Util.AccountNumberGenerator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private static final String not_owner_msg = "The account doesn't belong to the logged-in user";

    private String getEmailOfUser(){
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName(); // fetching the email of the user that logins...
    }

    private Account generateAndCheckAccount(Long accountID) throws AccessDeniedException {
        String email = getEmailOfUser();
        Account account = accountRepository.findById(accountID)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        if (!account.getUser().getEmail().equals(email)) {
            log.error(not_owner_msg);
            throw new AccessDeniedException("Not Allowed");
        }
        return account;
    }

    @Transactional
    public Long saveNewAccount(Account newAccount) {
        String email = getEmailOfUser();

        // Extracting the user if it exists...
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // restriction for a child account...
        if (newAccount.getAccountType() == AccountType.CHILD && user.getAge() >= 18) {
            log.error("A customer must have age below then 18 to have an child account");
            throw new RuntimeException("Child account allowed only for users below 18");
        }

        newAccount.setUser(user);
        newAccount.setAccountNumber("TemporaryNumber");

        // 1️. Saved first to get ID...
        Account savedAccount = accountRepository.save(newAccount);

        // 2️. Generating account number...
        String newAccountNumber = AccountNumberGenerator.generateAccountNumber(savedAccount);
        savedAccount.setAccountNumber(newAccountNumber);

        // 3️. Then saving again...
        accountRepository.save(savedAccount);
        return savedAccount.getId();
    }

    public Account getTheAccountById(Long accountID) throws AccessDeniedException {
        return generateAndCheckAccount(accountID);
    }

    @Transactional
    public boolean blockTheAccountById(Long accountID) throws AccessDeniedException {
        Account savedAccount = generateAndCheckAccount(accountID);
        if (savedAccount.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            return false;
        }
        savedAccount.setAccountStatus(AccountStatus.BLOCKED);
        accountRepository.save(savedAccount);
        return true;
    }

    @Transactional
    public boolean activateTheAccountById(Long accountID) throws AccessDeniedException {
        Account savedAccount = generateAndCheckAccount(accountID);

        if (savedAccount.getAccountStatus().equals(AccountStatus.ACTIVE)) {
            return false;
        }
        savedAccount.setAccountStatus(AccountStatus.ACTIVE);
        accountRepository.save(savedAccount);
        return true;
    }

    @Transactional
    public boolean closeTheAccountById(Long accountID) throws AccessDeniedException {
        Account savedAccount = generateAndCheckAccount(accountID);

        if (savedAccount.getAccountStatus().equals(AccountStatus.CLOSED)) {
            return false;
        }

        savedAccount.setAccountStatus(AccountStatus.CLOSED);
        accountRepository.save(savedAccount);
        return true;
    }

}
