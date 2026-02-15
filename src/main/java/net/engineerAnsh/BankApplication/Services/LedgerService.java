package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Entity.LedgerEntry;
import net.engineerAnsh.BankApplication.Enum.LedgerEntryType;
import net.engineerAnsh.BankApplication.Repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public void recordTransfer(
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            String transactionReference,
            String description
    ) {
        // Debit from sender...
        LedgerEntry debitEntry = LedgerEntry.builder()
                .account(fromAccount)
                .transactionReference(transactionReference)
                .amount(amount)
                .entryType(LedgerEntryType.DEBIT)
                .description(description)
                .build();

        // Credit to receiver...
        LedgerEntry creditEntry = LedgerEntry.builder()
                .account(toAccount)
                .transactionReference(transactionReference)
                .amount(amount)
                .entryType(LedgerEntryType.CREDIT)
                .description(description)
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);
    }

    @Transactional
    public void recordDeposit(
            Account account,
            BigDecimal amount,
            String transactionReference,
            String description
    ) {
        LedgerEntry creditEntry = LedgerEntry.builder()
                .account(account)
                .transactionReference(transactionReference)
                .amount(amount)
                .entryType(LedgerEntryType.CREDIT)
                .description(description)
                .build();

        ledgerEntryRepository.save(creditEntry);
    }

    @Transactional
    public void recordWithdrawal(
            Account account,
            BigDecimal amount,
            String transactionReference,
            String description
    ) {
        LedgerEntry debitEntry = LedgerEntry.builder()
                .account(account)
                .transactionReference(transactionReference)
                .amount(amount)
                .entryType(LedgerEntryType.DEBIT)
                .description(description)
                .build();

        ledgerEntryRepository.save(debitEntry);
    }

    // This method will return the true financial method...
    public BigDecimal calculateAccountBalance(String accountNumber) {
        BigDecimal balance =
                ledgerEntryRepository.calculateBalanceByAccountNumber(accountNumber);

        return balance != null ? balance : BigDecimal.ZERO;
    }

}
