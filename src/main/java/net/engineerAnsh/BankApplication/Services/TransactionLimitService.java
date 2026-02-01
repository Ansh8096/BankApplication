package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Config.TransactionLimitConfig;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionLimitService {

    private final TransactionLimitConfig limitConfig;

    public void validatePerTransactionLimit(TransactionType type, BigDecimal amount){

        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new RuntimeException("Amount must be greater than 0");
        }

        BigDecimal limit = switch (type){
            case DEPOSIT -> limitConfig.getDepositLimit();
            case WITHDRAW -> limitConfig.getWithdrawLimit();
            case TRANSFER -> limitConfig.getTransferLimit();
        };

        if(amount.compareTo(limit) > 0){
            throw new RuntimeException(type + " per transaction limit exceeded. Max allowed: ₹" + limit);
        }
    }

}
