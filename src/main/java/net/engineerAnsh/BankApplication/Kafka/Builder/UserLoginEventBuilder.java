package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserLoginEventBuilder {

    public UserLoginEvent buildLoginEvent(String email, String ip, String userAgent){
        return new UserLoginEvent(
                email,
                ip,
                userAgent,
                LocalDateTime.now()
        );
    }
}
