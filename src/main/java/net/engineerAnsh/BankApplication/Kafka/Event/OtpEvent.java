package net.engineerAnsh.BankApplication.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.engineerAnsh.BankApplication.Kafka.Enums.OtpType;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpEvent implements UserEvent{

    private String otpId;
    private String phoneNumber;
    private OtpType type;
    private LocalDateTime occurredAt;

    @Override
    public String getEventType() {
        return "OTP";
    }
}
