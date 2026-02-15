package net.engineerAnsh.BankApplication.Dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class DepositRequest {

    @NotBlank
    private String accountNo;

    @NotNull
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank
    private String clientTransactionId;

    private String remark;
}
