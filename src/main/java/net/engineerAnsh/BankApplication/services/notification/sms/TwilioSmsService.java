package net.engineerAnsh.BankApplication.services.notification.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Utils.MaskingUtil;
import net.engineerAnsh.BankApplication.services.auth.RedisOtpService;
import net.engineerAnsh.BankApplication.services.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioSmsService implements NotificationService {

    private final RedisOtpService redisOtpService;

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    @Value("${otp.message-template}")
    private String messageTemplate;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    @Override
    public void sendOtp(String phone, String otpId) {

        String otp = redisOtpService.getOtpById(otpId);

        if (otp == null) {
            log.error("OTP expired for otpId={}", otpId);
            return;
        }

        String message = String.format(messageTemplate, otp);

        Message.creator(
                new PhoneNumber(phone),     // to-number
                new PhoneNumber(fromNumber), // from-number (Twilio)
                message
        ).create();

        log.info("An otp with otpId: {}, is sent successfully via SMS to: {}", otpId, MaskingUtil.maskPhone(phone));

    }

}