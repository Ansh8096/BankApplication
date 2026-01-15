package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Enum.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public void checkIfAccountBelongsToUser(Account from) throws AccessDeniedException {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        if(!from.getUser().getEmail().equals(email)){
           throw new AccessDeniedException("This Account doesn't belongs to you");
        }
    }

    @Transactional
    public void transferMoney(
            Account from,
            Account to,
            BigDecimal amount,
            String remark
    ) throws AccessDeniedException {

        // If both accounts belong to same user it will consider the self-pay option

        // Prevent same-account transfer...
        if(from.getId().equals(to.getId())){
            throw new RuntimeException("Cannot transfer to same account");
        }

        checkIfAccountBelongsToUser(from);

        // how 'compareTo()' works:-->
        // if(balance < amount): result -1
        // if(balance == amount):result 0
        // if(balance > amount): result 1
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

}
