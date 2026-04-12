package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Kafka.Enums.OtpType;
import net.engineerAnsh.BankApplication.Kafka.Event.OtpEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OtpEventBuilder {

    public OtpEvent buildOtpEvent(String phone, String otpId, OtpType otpType){
        return new OtpEvent(
                otpId,
                phone,
                otpType,
                LocalDateTime.now()
        );
    }
}
