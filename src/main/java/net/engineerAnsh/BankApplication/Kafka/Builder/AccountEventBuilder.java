package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Enum.AccountEventType;
import net.engineerAnsh.BankApplication.Kafka.Event.AccountNotificationEvent;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountEventBuilder {

    public AccountNotificationEvent buildAccountEvent(
            Account account, AccountEventType eventType) {
        return new AccountNotificationEvent(
                UUID.randomUUID().toString(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getUser().getEmail(),
                eventType,
                LocalDateTime.now()
        );
    }
}
