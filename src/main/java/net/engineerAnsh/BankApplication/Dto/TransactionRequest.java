package net.engineerAnsh.BankApplication.Dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class TransactionRequest {

    private Long fromAccountId;

    private Long toAccountId;

    private BigDecimal amount;

    private String remark;

}
