package net.engineerAnsh.BankApplication.services.notification.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEmailRequest {
    private BigDecimal amount;
    private String accountNumber;
    private String date;
    private String txnId;
    private String remark;
}