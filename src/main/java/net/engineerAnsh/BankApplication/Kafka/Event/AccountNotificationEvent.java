package net.engineerAnsh.BankApplication.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.engineerAnsh.BankApplication.Enum.account.AccountEventType;
import net.engineerAnsh.BankApplication.Enum.account.AccountType;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountNotificationEvent {
    private String eventId;
    private String accountNumber;
    private AccountType accountType;
    private String email;
    private AccountEventType eventType;
    private LocalDateTime timestamp;
}