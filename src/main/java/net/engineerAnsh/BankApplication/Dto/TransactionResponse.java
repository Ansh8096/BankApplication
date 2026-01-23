package net.engineerAnsh.BankApplication.Dto;

import lombok.*;
import net.engineerAnsh.BankApplication.Enum.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long transactionId;

    private String fromAccountNo;

    private String toAccountNo;

    private BigDecimal amount;

    private TransactionType type;

    private TransactionStatus status;

    private String remark;

    private LocalDateTime createdAt;
}
