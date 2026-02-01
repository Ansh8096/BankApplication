package net.engineerAnsh.BankApplication.Services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Dto.Statements.StatementRowDto;
import net.engineerAnsh.BankApplication.Dto.transaction.TransactionResponse;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import net.engineerAnsh.BankApplication.Utils.AccountMaskingUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final StatementBuilder statementBuilder;
    private final TransactionLimitService transactionLimitService;

    private Account findTheActiveAccount(String accountNumber) {
        return accountRepository.findByAccountNumberAndAccountStatus(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("No active account found"));
    }

    public Account findActiveAccountAndValidate(String accountNumber) throws AccessDeniedException {
        String email = accountService.getEmailOfLoggedInUser();
        Account account = accountRepository.findByAccountNumberAndAccountStatus(accountNumber, AccountStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Account not found or is not active"));
        // If the loginUserEmail is not equal to the userEmail that belongs to account, then throw an exception...
        if (!account.getUser().getEmail().equals(email)) {
            log.error(AccountService.getNot_owner_msg());
            throw new AccessDeniedException(AccountService.getNot_owner_msg());
        }
        return account;
    }

    // I will be using this method when we want to fetch transaction by reference number...
    private TransactionResponse mapToTransactionResponse(Transaction txn) {
        // I'm able to set all the values in the constructor like this, because of the notations of constructors on 'TransactionResponse'...
        return new TransactionResponse(
                txn.getId(),
                txn.getFromAccount() != null
                        ? AccountMaskingUtil.maskAccountNumber(
                        txn.getFromAccount().getAccountNumber())
                        : null,
                txn.getToAccount() != null
                        ? AccountMaskingUtil.maskAccountNumber(
                        txn.getToAccount().getAccountNumber())
                        : null,
                txn.getAmount(),
                txn.getType(),
                txn.getStatus(),
                txn.getRemark(),
                txn.getCreatedAt()
        );
    }

    private BigDecimal calculateOpeningBalanceForTransactions(
            String accountNumber,
            LocalDate from
    ) {

        List<Transaction> previousTransactionsBeforeDate = transactionRepository.findAllSuccessfulTransactionsBeforeDate(
                accountNumber, from.atStartOfDay());

        previousTransactionsBeforeDate.sort(
                Comparator.comparing(Transaction::getCreatedAt)
        );

        BigDecimal openingBalance = BigDecimal.ZERO;

        // Maintaining the opening balance according to the credit or debit transactions...
        for (Transaction tx : previousTransactionsBeforeDate) {

            // Ignore FAILED / REVERSED transactions...
            if (tx.getStatus() != TransactionStatus.SUCCESS) {
                continue;
            }
            if (tx.getFromAccount() != null && tx.getFromAccount().getAccountNumber().equals(accountNumber)) {
                // debit...
                openingBalance = openingBalance.subtract(tx.getAmount());
            } else if (tx.getToAccount() != null && tx.getToAccount().getAccountNumber().equals(accountNumber)) {
                // credit...
                openingBalance = openingBalance.add(tx.getAmount());
            }
        }

        log.info("Opening balance = {}", openingBalance);
        return openingBalance;
    }

    @Transactional
    public void transferMoneyBetweenAccounts(
            String fromAccountNo,
            String toAccountNo,
            BigDecimal amount,
            String remark
    ) throws AccessDeniedException {

        // Prevent same-account transfer...
        if (fromAccountNo.equals(toAccountNo)) {
            throw new RuntimeException("Cannot transfer to same account");
        }

        // Finding the active accounts via their account number...
        Account from = findActiveAccountAndValidate(fromAccountNo); // We also checks if the 'from' account belongs to loggedIn user or not...
        Account to = findTheActiveAccount(toAccountNo); // Verifying if the 'to' account exists or not...

        // Throw exception if given 'amount' <= zero or exceeding the maximum transfer limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.TRANSFER, amount);

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

        // Throw exception if given 'amount' <= zero or exceeding the maximum deposit limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.DEPOSIT, amount);

        // Update the balance...
        account.setAccountBalance(account.getAccountBalance().add(amount));

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setToAccount(account);
        txn.setType(TransactionType.DEPOSIT);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        String description = (remark != null && !remark.isEmpty()) ? remark : "Deposited money";
        txn.setRemark(description);

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
        // Throw exception if given 'amount' <= zero or exceeding the maximum withdraw limit...
        transactionLimitService.validatePerTransactionLimit(TransactionType.WITHDRAW, amount);

        // Throw exception if given amount is greater than the account balance...
        if (account.getAccountBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient Balance");
        }

        // Update the balance...
        account.setAccountBalance(account.getAccountBalance().subtract(amount));

        // Creating new transaction object to save the record in the table...
        Transaction txn = new Transaction();
        txn.setFromAccount(account);
        txn.setType(TransactionType.WITHDRAW);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setAmount(amount);
        if (remark != null && !remark.isEmpty()) {
            txn.setRemark(remark);
        }

        // save the transaction:
        transactionRepository.save(txn);

        // save account:
        accountRepository.save(account);
    }


    private AccountStatementDto generateStatementInternal(
            Account account,
            String accountNumber,
            LocalDate from,
            LocalDate to
    ) {

        // Calculating the opening balance (accountBalance on that time),before from day...
        BigDecimal openingBalance = calculateOpeningBalanceForTransactions(accountNumber, from);

        // Define Time window...
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); // Exclusive...

        // Fetch all the valid transactions within the given time...
        List<Transaction> transactions = transactionRepository.findStatementTransactions(accountNumber, start, end);

        // Build rows of statements...
        List<StatementRowDto> statementRows = statementBuilder.buildStatementRows(
                transactions,
                accountNumber,
                openingBalance
        );

        //  Return object of the accountStatement DTO...
        return new AccountStatementDto(
                "BANK OF ANSH",
                account.getUser().getName(),
                AccountMaskingUtil.maskAccountNumber(accountNumber),
                account.getAccountType().toString(),
                account.getIfscCode(),
                from,
                to,
                openingBalance,
                statementRows,
                statementRows.isEmpty()
                        ? openingBalance
                        : statementRows.get(statementRows.size() - 1).getBalance()
        );
    }

    @Transactional(readOnly = true)
    public AccountStatementDto generateStatement(
            String accountNumber,
            LocalDate from,
            LocalDate to
    ) {
        // Validate account ownership (API use case)...
        Account account = findActiveAccountAndValidate(accountNumber);

        // Calling the internal implementation of the
        return generateStatementInternal(account,accountNumber,from,to);
    }

    @Transactional(readOnly = true)
    public AccountStatementDto generateMonthlyStatement(
            String accountNumber,
            LocalDate from,
            LocalDate to
    ) {
        // NO ownership validation (scheduler use case)...
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("An error occurred..."));
        return generateStatementInternal(account,accountNumber,from,to);
    }
}








// How 'compareTo()' works:-
// if(balance < amount): result '-1',
// if(balance == amount): result '0',
// if(balance > amount): result '1'