package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Kafka.Event.UserRegisteredEvent;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserRegisteredEventBuilder {

    public UserRegisteredEvent buildRegistrationEvent(String email, String tokenValue){
        return new UserRegisteredEvent(
                email,
                tokenValue,
                LocalDateTime.now()
        );
    }
}
