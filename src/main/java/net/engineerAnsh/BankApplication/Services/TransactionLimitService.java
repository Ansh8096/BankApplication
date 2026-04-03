package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Config.TransactionLimitConfig;
import net.engineerAnsh.BankApplication.Enum.transaction.TransactionType;
import net.engineerAnsh.BankApplication.Repository.TransactionRepository;
import net.engineerAnsh.BankApplication.Utils.DateRangeUtil;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionLimitService {

    private final TransactionLimitConfig limitConfig;
    private final TransactionRepository transactionRepository;

    public void validatePerTransactionLimit(TransactionType type, BigDecimal amount){

        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new RuntimeException("Amount must be greater than 0");
        }

        BigDecimal limit = switch (type){
            case DEPOSIT -> limitConfig.getDepositPerTxn();
            case WITHDRAW -> limitConfig.getWithdrawPerTxn();
            case TRANSFER -> limitConfig.getTransferPerTxn();
        };

        if(amount.compareTo(limit) > 0){
            throw new RuntimeException(type + " per transaction limit exceeded. Max allowed: ₹" + limit); // create custom exception: TransactionLimitExceededException...
        }
    }

    public void validateDailyTransactionLimit(TransactionType type, BigDecimal amount, String accountNumber) {

        LocalDateTime start = DateRangeUtil.startOfTodayIST();
        LocalDateTime end = DateRangeUtil.startOfTomorrowIST();

        // fetching the daily limit according to the txn type...
        BigDecimal dailyLimit = switch (type) {
            case DEPOSIT -> limitConfig.getDepositDaily();
            case TRANSFER -> limitConfig.getTransferDaily();
            case WITHDRAW -> limitConfig.getWithdrawDaily();
        };

        // Setting direction according to the transaction type...
        String direction = switch (type) {
            case DEPOSIT -> "CREDIT"; // deposit affects toAccount
            case TRANSFER, WITHDRAW -> "DEBIT"; // withdrawals or transfers affect fromAccount
        };

        // It will give me the total sum of all the 'debit' or 'credit' transactions of a given accountNumber...
        BigDecimal todayTotal = transactionRepository.sumDailyAmount(
                type, direction, accountNumber, start, end
        );

        // Throw an error, if the dailyLimit is reached (if we performed the specific txn)...
        if (todayTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new RuntimeException(
                    type + " daily limit exceeded. Today used: ₹" + todayTotal +
                            ", Requested: ₹" + amount +
                            ", Daily Limit: ₹" + dailyLimit
            );
        }
    }

}
