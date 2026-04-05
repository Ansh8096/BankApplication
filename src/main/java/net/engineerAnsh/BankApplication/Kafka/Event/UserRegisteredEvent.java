package net.engineerAnsh.BankApplication.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisteredEvent implements UserEvent {

    private String email;
    private String verificationToken;
    private LocalDateTime occurredAt;

    @Override
    public String getEventType() {
        return "USER_REGISTERED";
    }

}