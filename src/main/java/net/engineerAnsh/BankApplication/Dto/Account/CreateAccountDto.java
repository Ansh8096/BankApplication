package net.engineerAnsh.BankApplication.Dto.Account;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import net.engineerAnsh.BankApplication.Enum.account.AccountType;

@Getter
@Setter
public class CreateAccountDto {

    @NotNull(message = "accountType is required")
    private AccountType accountType;
}
