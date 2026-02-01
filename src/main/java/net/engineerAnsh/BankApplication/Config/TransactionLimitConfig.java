package net.engineerAnsh.BankApplication.Config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Getter
@Component
public class TransactionLimitConfig {

    private final BigDecimal depositLimit;
    private final BigDecimal withdrawLimit;
    private final BigDecimal transferLimit;

    // This constructor will automatically fetch limits from the 'yaml' file...
    public TransactionLimitConfig(
            @Value("${limit.deposit.perTxn}") BigDecimal depositLimit,
            @Value("${limit.withdraw.perTxn}") BigDecimal withdrawLimit,
            @Value("${limit.transfer.perTxn}") BigDecimal transferLimit)
    {
        this.depositLimit = depositLimit;
        this.withdrawLimit = withdrawLimit;
        this.transferLimit = transferLimit;
    }
}
