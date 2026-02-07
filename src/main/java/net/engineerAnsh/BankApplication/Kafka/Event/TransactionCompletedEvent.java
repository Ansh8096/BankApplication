package net.engineerAnsh.BankApplication.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TransactionCompletedEvent {

    private String transactionReference;
    private String type;
    private String status;
    private BigDecimal amount;

    private String fromAccountMasked;
    private String toAccountMasked;

    private LocalDateTime createdAt;

    private String userEmail;
    private String remark;
}
