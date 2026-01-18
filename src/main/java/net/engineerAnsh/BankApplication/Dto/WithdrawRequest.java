package net.engineerAnsh.BankApplication.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class WithdrawRequest {

    @NotBlank
    private String accountNo;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String remark;
}
