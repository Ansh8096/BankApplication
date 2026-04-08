package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserLoginEventBuilder {

    public UserLoginEvent buildLoginEvent(String userName, String email, String ip, String userAgent, String location){
        return new UserLoginEvent(
                userName,
                email,
                ip,
                location,
                userAgent,
                LocalDateTime.now()
        );
    }
}
