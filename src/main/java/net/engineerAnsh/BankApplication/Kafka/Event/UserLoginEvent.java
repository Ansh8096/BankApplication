package net.engineerAnsh.BankApplication.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginEvent implements UserEvent{
    private String userName;
    private String email;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime occurredAt;

    @Override
    public String getEventType() {
        return "USER_LOGIN";
    }
}
