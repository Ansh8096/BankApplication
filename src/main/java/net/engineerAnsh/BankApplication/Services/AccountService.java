package net.engineerAnsh.BankApplication.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.*;
import net.engineerAnsh.BankApplication.Kafka.Event.AccountNotificationEvent;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.OutboxEventRepository;
import net.engineerAnsh.BankApplication.Repository.UserRepository;
import net.engineerAnsh.BankApplication.Utils.AccountNumberGenerator;
import net.engineerAnsh.BankApplication.exception.KycNotVerifiedException;
import org.apache.coyote.BadRequestException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxRepository;

    @Getter
    private static final String not_owner_msg = "The account doesn't belong to the logged-in user";

    public String getEmailOfLoggedInUser() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName(); // fetching the email of the user that logins...
    }

    // Use Only when we want to activate, block, close the account...
    public Account findNotClosedAccountAndValidate(String email, String accountNumber) throws AccessDeniedException {
        userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found..."));

        Account account = accountRepository.findByAccountNumberAndAccountStatusNot(accountNumber, AccountStatus.CLOSED)
                .orElseThrow(() -> new EntityNotFoundException("No active account found..."));
        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        if (!account.getUser().getEmail().equals(email)) {
            log.error(not_owner_msg);
            throw new AccessDeniedException(not_owner_msg);
        }
        return account;
    }

    private Account findAccountForAdmin(String accountNumber) {
        return accountRepository.findByAccountNumberAndAccountStatusNot(accountNumber, AccountStatus.CLOSED)
                .orElseThrow(() -> new EntityNotFoundException("No active account found..."));
    }

    private void saveOutboxEvent(Object event)
            throws JsonProcessingException {

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventType(OutboxEventType.ACCOUNT_NOTIFICATION)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxRepository.save(outboxEvent);
    }

    private void publishAccountNotificationEvent(Account account, AccountEventType eventType)
            throws JsonProcessingException {

        AccountNotificationEvent event = new AccountNotificationEvent(
                UUID.randomUUID().toString(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getUser().getEmail(),
                eventType,
                LocalDateTime.now()
        );

        saveOutboxEvent(event);
    }

    private void validateAccountCreation(User user, AccountType accountType) {
        if (!accountType.isSinglePerUser()) {
            return; // LOAN allowed multiple...
        }
        boolean exists = accountRepository
                .existsByUserAndAccountTypeAndAccountStatusNot(
                        user,
                        accountType,
                        AccountStatus.CLOSED
                );
        if (exists) {
            throw new IllegalStateException(
                    "User already has an active " + accountType + " account"
            );
        }
    }

    private void kycCheck(KycStatus status){
        if(status != KycStatus.APPROVED){
            throw new KycNotVerifiedException("KYC verification required");
        }
    }

    @Transactional
    public Long saveNewAccount(String email, AccountType accountType) throws BadRequestException, JsonProcessingException {

        // Extracting the user if it exists...
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Kyc check...
        kycCheck(user.getKycStatus());

        // validate the account type...
        validateAccountCreation(user, accountType);

        // Restriction for user who have age below 18...
        if (accountType != AccountType.CHILD && user.getAge() < 18) {
            throw new BadRequestException("User below the age 18 are only allowed to create child account");
        }

        // restriction for a child account...
        if (accountType == AccountType.CHILD && user.getAge() >= 18) {
            log.error("A customer must have age below then 18 to have an child account");
            throw new BadRequestException("Child account allowed only for users below 18");
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

        publishAccountNotificationEvent(savedAccount, AccountEventType.ACCOUNT_CREATED);

        return savedAccount.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // " 'Propagation.REQUIRES_NEW' means Pause the current transaction, start a NEW independent transaction for this method"
    public void freezeTheAccountByAccountNumber(String email, String accountNumber) throws AccessDeniedException, JsonProcessingException {
        Account savedAccount = findNotClosedAccountAndValidate(email, accountNumber);

        if (savedAccount.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            throw new IllegalStateException("Account is BLocked, contact the administrator...");
        }

        if (savedAccount.getAccountStatus() == AccountStatus.FROZEN) {
            log.info("Account is already frozen...");
            return;
        }

        savedAccount.setAccountStatus(AccountStatus.FROZEN);

        publishAccountNotificationEvent(savedAccount, AccountEventType.ACCOUNT_FROZEN);
    }

    @PreAuthorize("hasRole('ADMIN')") // Even if someone bypasses the controller, service is protected...
    @Transactional
    public void blockTheAccountByAccountNumber(String accountNumber) throws AccessDeniedException, JsonProcessingException {

        Account savedAccount = findAccountForAdmin(accountNumber);

        if (savedAccount.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new IllegalStateException("Account is already blocked...");
        }

        savedAccount.setAccountStatus(AccountStatus.BLOCKED);
        accountRepository.save(savedAccount);

        publishAccountNotificationEvent(savedAccount, AccountEventType.ACCOUNT_BLOCKED);
    }

    @PreAuthorize("hasRole('ADMIN')") // Even if someone bypasses the controller, service is protected...
    @Transactional
    public void activateTheAccount(String accountNumber) throws AccessDeniedException, JsonProcessingException {

        Account savedAccount = findAccountForAdmin(accountNumber);

        if (savedAccount.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is already active...");
        }

        savedAccount.setAccountStatus(AccountStatus.ACTIVE);
        accountRepository.save(savedAccount);

        publishAccountNotificationEvent(savedAccount, AccountEventType.ACCOUNT_ACTIVATED);
    }

    @PreAuthorize("hasRole('ADMIN')") // Even if someone bypasses the controller, service is protected...
    @Transactional
    public void closeTheAccount(String accountNumber) throws AccessDeniedException, JsonProcessingException {
        Account savedAccount = findAccountForAdmin(accountNumber);

        if (savedAccount.getAccountStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed...");
        }

        savedAccount.setAccountStatus(AccountStatus.CLOSED);
        savedAccount.setAccountClosedAt(LocalDateTime.now());
        accountRepository.save(savedAccount);

        publishAccountNotificationEvent(savedAccount, AccountEventType.ACCOUNT_CLOSED);
    }

    @Transactional
    public Account getTheAccountByAccountNumber(String email, String accountNumber) throws AccessDeniedException {
        return findNotClosedAccountAndValidate(email, accountNumber);
    }

    @Transactional
    public BigDecimal getTheAccountBalanceByAccountNumber(String email, String accountNumber) throws AccessDeniedException {
        Account savedAccount = findNotClosedAccountAndValidate(email, accountNumber);
        return ledgerService.calculateAccountBalance(savedAccount.getAccountNumber());
    }
}
