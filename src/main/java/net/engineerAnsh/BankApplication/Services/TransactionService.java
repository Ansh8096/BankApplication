package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    private Account findTheActiveAccount(String accountNumber){
        return accountRepository.findByAccountNumberAndAccountStatus(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(()-> new UsernameNotFoundException("No active account found"));
    }

    public Account findActiveAccountAndValidate(String accountNumber) throws AccessDeniedException {
        String email = accountService.getEmailOfLoggedInUser();
        Account account = accountRepository.findByAccountNumberAndAccountStatus(accountNumber,AccountStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Account not found or is not active"));
        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        if (!account.getUser().getEmail().equals(email)) {
            log.error(AccountService.getNot_owner_msg());
            throw new AccessDeniedException(AccountService.getNot_owner_msg());
        }
        return account;
    }

    @Transactional
    public void transferMoneyBetweenAccounts(
            String fromAccountNo,
            String toAccountNo,
            BigDecimal amount,
            String remark
    ) throws AccessDeniedException {

        // Prevent same-account transfer...
        if(fromAccountNo.equals(toAccountNo)){
            throw new RuntimeException("Cannot transfer to same account");
        }

        // Finding the active accounts via their account number...
        Account from = findActiveAccountAndValidate(fromAccountNo); // We also checks if the 'from' account belongs to loggedIn user or not...
        Account to = findTheActiveAccount(toAccountNo); // Verifying if the 'to' account exists or not...

        // Throw error if the 'from' Account balance is low...
        if (from.getAccountBalance().compareTo(amount) < 0) { // Why compareTo(), Because BigDecimal does not support '<'
            throw new RuntimeException("Insufficient balance"); // use custom exception such as : 'InsufficientBalanceException'
        }

        // debit amount from 'fromAccount'
        from.setAccountBalance(from.getAccountBalance().subtract(amount));

        // credit amount to 'toAccount'
        to.setAccountBalance(to.getAccountBalance().add(amount));

        // Creating new transaction object to save the record in the table...
        Transaction tnx = new Transaction();
        tnx.setAmount(amount);
        tnx.setFromAccount(from);
        tnx.setToAccount(to);
        tnx.setRemark(remark);
        tnx.setType(TransactionType.TRANSFER);
        tnx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(tnx); // save the transaction...

        // save accounts:
        accountRepository.save(from);
        accountRepository.save(to);
    }

    @Transactional
    public void depositMoneyToTheAccount(String toAccountNo,
                                         BigDecimal amount,
                                         String remark) throws AccessDeniedException {

        // Finding the active account via account number...
        Account account = findActiveAccountAndValidate(toAccountNo); // We also checks if the 'to' account belongs to loggedIn user or not...

        // Throw exception if given amount is equal or smaller than zero...
        if(amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new RuntimeException("Invalid deposit amount");
        }

        // Update the balance...
        account.setAccountBalance(account.getAccountBalance().add(amount));

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setToAccount(account);
        txn.setType(TransactionType.DEPOSIT);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        if(remark != null && !remark.isEmpty()) {
            txn.setRemark(remark);
        }

        // save the transaction:
        transactionRepository.save(txn);

        // save account:
        accountRepository.save(account);
    }

    @Transactional
    public void withdrawMoneyFromTheAccount(String fromAccountNo,
                                            BigDecimal amount,
                                            String remark) throws AccessDeniedException {

        // Finding the active account via account number...
        Account account = findActiveAccountAndValidate(fromAccountNo); // We also checks if the 'from' account belongs to loggedIn user or not...

        // Throw exception if given amount is equal or smaller than zero...
        if(amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new RuntimeException("Invalid withdraw amount");
        }

        // Throw exception if given amount is greater than the account balance...
        if(account.getAccountBalance().compareTo(amount) < 0){
            throw new RuntimeException("Insufficient Balance");
        }

        // Update the balance...
        account.setAccountBalance(account.getAccountBalance().subtract(amount));

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setFromAccount(account);
        txn.setType(TransactionType.WITHDRAWAL);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        if(remark != null && !remark.isEmpty()) {
            txn.setRemark(remark);
        }

        // save the transaction:
        transactionRepository.save(txn);

        // save account:
        accountRepository.save(account);
    }

    @Transactional
    public List<Transaction> getTransactionsHistoryOfTheAccount(String accountNo) throws AccessDeniedException {

        // Finding the active account via account number...
        findActiveAccountAndValidate(accountNo); // We also checks if the account belongs to loggedIn user or not...

        return transactionRepository.findByFromAccount_AccountNumberOrToAccount_AccountNumber( // it returns: List<Transaction>
                accountNo,
                accountNo
        );
    }



}





// How 'compareTo()' works:-
// if(balance < amount): result '-1',
// if(balance == amount): result '0',
// if(balance > amount): result '1'