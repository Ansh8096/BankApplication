package net.engineerAnsh.BankApplication.Fraud;

import lombok.Builder;
import lombok.Getter;
import net.engineerAnsh.BankApplication.Enum.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionContext {

    private Long userId;

    private String name;

    private String email;

    private String accountNumber;

    private BigDecimal amount;

    private TransactionType transactionType;

    private LocalDateTime timestamp;

    private String ipAddress;
}