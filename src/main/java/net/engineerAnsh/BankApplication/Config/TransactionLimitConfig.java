package net.engineerAnsh.BankApplication.Config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Getter
@Component
public class TransactionLimitConfig {

    private final BigDecimal depositPerTxn;
    private final BigDecimal withdrawPerTxn;
    private final BigDecimal transferPerTxn;

    private final BigDecimal depositDaily;
    private final BigDecimal withdrawDaily;
    private final BigDecimal transferDaily;

    // This constructor will automatically fetch limits from the 'yaml' file...
    public TransactionLimitConfig(
            @Value("${limit.deposit.perTxn}") BigDecimal depositPerTxn,
            @Value("${limit.withdraw.perTxn}") BigDecimal withdrawPerTxn,
            @Value("${limit.transfer.perTxn}") BigDecimal transferPerTxn,

            @Value("${limit.deposit.daily}") BigDecimal depositDaily,
            @Value("${limit.withdraw.daily}") BigDecimal withdrawDaily,
            @Value("${limit.transfer.daily}") BigDecimal transferDaily
    ) {
        this.depositPerTxn = depositPerTxn;
        this.withdrawPerTxn = withdrawPerTxn;
        this.transferPerTxn = transferPerTxn;
        this.depositDaily = depositDaily;
        this.withdrawDaily = withdrawDaily;
        this.transferDaily = transferDaily;
    }
}
