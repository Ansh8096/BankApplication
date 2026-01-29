package net.engineerAnsh.BankApplication.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import net.engineerAnsh.BankApplication.Enum.AccountType;

@Getter
@Setter
public class CreateAccountDto {

    @NotNull(message = "accountType is required")
    private AccountType accountType;
}
