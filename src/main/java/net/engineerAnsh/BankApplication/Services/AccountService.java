package net.engineerAnsh.BankApplication.Services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.AccountType;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
import net.engineerAnsh.BankApplication.Utils.AccountNumberGenerator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    @Getter
    private static final String not_owner_msg = "The account doesn't belong to the logged-in user";

    public String getEmailOfLoggedInUser(){
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName(); // fetching the email of the user that logins...
    }

    // Use Only when we want to activate, block, close the account
    public Account findNotClosedAccountAndValidate(String accountNumber) throws AccessDeniedException {
        String email = getEmailOfLoggedInUser();
        Account account = accountRepository.findByAccountNumberAndAccountStatusNot(accountNumber,AccountStatus.CLOSED)
                .orElseThrow(() -> new RuntimeException("Account not found or is closed"));
        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        if (!account.getUser().getEmail().equals(email)) {
            log.error(not_owner_msg);
            throw new AccessDeniedException(not_owner_msg);
        }
        return account;
    }

    @Transactional
    public Long saveNewAccount(AccountType accountType) {
        String email = getEmailOfLoggedInUser();

        // Extracting the user if it exists...
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Restriction for user who have age below 18...
        if(accountType != AccountType.CHILD && user.getAge() < 18){
            throw new RuntimeException("User below the age 18 are only allowed to create child account");
        }

        // restriction for a child account...
        if (accountType == AccountType.CHILD && user.getAge() >= 18) {
            log.error("A customer must have age below then 18 to have an child account");
            throw new RuntimeException("Child account allowed only for users below 18");
        }

        Account account = new Account();

        account.setAccountType(accountType);
        account.setUser(user);
        account.setAccountNumber("TemporaryNumber");

        // 1️. Saving first to get ID...
        Account savedAccount = accountRepository.save(account);

        // 2️. Generating account number...
        String newAccountNumber = AccountNumberGenerator.generateAccountNumber(savedAccount);
        savedAccount.setAccountNumber(newAccountNumber);

        // 3️. Then saving again...
        accountRepository.save(savedAccount);
        return savedAccount.getId();
    }

    public Account getTheAccountByAccountNumber(String accountNumber) throws AccessDeniedException {
        return findNotClosedAccountAndValidate(accountNumber);
    }

    @Transactional
    public boolean blockTheAccountByAccountNumber(String accountNumber) throws AccessDeniedException {
        Account savedAccount = findNotClosedAccountAndValidate(accountNumber);
        if (savedAccount.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            return false;
        }
        savedAccount.setAccountStatus(AccountStatus.BLOCKED);
        accountRepository.save(savedAccount);
        return true;
    }

    @Transactional
    public boolean activateTheAccountByAccountNumber(String accountNumber) throws AccessDeniedException {
        Account savedAccount = findNotClosedAccountAndValidate(accountNumber);
        if (savedAccount.getAccountStatus().equals(AccountStatus.ACTIVE)) {
            return false;
        }
        savedAccount.setAccountStatus(AccountStatus.ACTIVE);
        accountRepository.save(savedAccount);
        return true;
    }

    @Transactional
    public boolean closeTheAccountByAccountNumber(String accountNumber) throws AccessDeniedException {
        Account savedAccount = findNotClosedAccountAndValidate(accountNumber);
        if (savedAccount.getAccountStatus().equals(AccountStatus.CLOSED)) {
            return false;
        }
        savedAccount.setAccountStatus(AccountStatus.CLOSED);
        savedAccount.setAccountClosedAt(LocalDateTime.now());
        accountRepository.save(savedAccount);
        return true;
    }

}
