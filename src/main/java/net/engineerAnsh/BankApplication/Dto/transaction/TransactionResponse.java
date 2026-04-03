package net.engineerAnsh.BankApplication.Dto.transaction;

import lombok.*;
import net.engineerAnsh.BankApplication.Enum.transaction.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String transactionReference;

    private String fromAccountNo;

    private String toAccountNo;

    private BigDecimal amount;

    private TransactionType type;

    private TransactionStatus status;

    private String remark;

    private LocalDateTime createdAt;
}
