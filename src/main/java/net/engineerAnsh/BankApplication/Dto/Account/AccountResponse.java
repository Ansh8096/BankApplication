package net.engineerAnsh.BankApplication.Dto.Account;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import net.engineerAnsh.BankApplication.Enum.account.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.account.AccountType;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponse {

    private String accountNumber;

    private String ifscCode;

    private AccountType accountType;

    private AccountStatus accountStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedAt;

}