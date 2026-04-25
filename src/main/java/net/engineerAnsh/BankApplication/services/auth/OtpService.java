package net.engineerAnsh.BankApplication.services.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Enum.outbox.OutboxEventType;
import net.engineerAnsh.BankApplication.Enum.user.PhoneVerificationStatus;
import net.engineerAnsh.BankApplication.Kafka.Builder.OtpEventBuilder;
import net.engineerAnsh.BankApplication.Kafka.Enums.OtpType;
import net.engineerAnsh.BankApplication.Kafka.Event.OtpEvent;
import net.engineerAnsh.BankApplication.Utils.MaskingUtil;
import net.engineerAnsh.BankApplication.exception.exceptions.AlreadyVerifiedException;
import net.engineerAnsh.BankApplication.exception.exceptions.InvalidOtpException;
import net.engineerAnsh.BankApplication.exception.exceptions.OtpBlockedException;
import net.engineerAnsh.BankApplication.exception.exceptions.OtpExpiredException;
import net.engineerAnsh.BankApplication.services.outbox.OutboxEventService;
import net.engineerAnsh.BankApplication.services.user.UserService;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisOtpService redisOtpService;
    private final RedisRateLimiterService rateLimiter;
    private final OtpEventBuilder otpEventBuilder;
    private final UserService userService;
    private final OutboxEventService outboxEventService;

    private void activateUserIfFullyVerified(User user) {
        if (user.isEmailVerified() &&
                user.getPhoneVerificationStatus() == PhoneVerificationStatus.VERIFIED) {

            user.setActive(true);
        }
    }

    private void phoneVerificationCheck(PhoneVerificationStatus status) {
        if (status == PhoneVerificationStatus.VERIFIED) {
            throw new AlreadyVerifiedException("Phone already verified");
        }
    }

    public void generateAndSendOtp(String phone) throws JsonProcessingException {

        User user = userService.findUserByPhone(phone);
        phoneVerificationCheck(user.getPhoneVerificationStatus());

        // 1. Rate limiting...
        if (!rateLimiter.isAllowed(phone)) {
            throw new OtpBlockedException("Too many OTP requests. Try again later.");
        }

        // 2️. Prevent resend spam (cooldown using TTL)...
        // Prevents multiple OTP generation...
        if (redisOtpService.otpExistsByPhone(phone)) {
            long ttl = redisOtpService.getOtpTtl(phone);
            throw new OtpBlockedException(
                    "OTP already sent. Please wait " + ttl + " seconds."
            );
        }

        // 2. Generate 6-digit OTP...
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        // 3. Store in Redis...
        String otpId = redisOtpService.saveOtpWithId(phone, otp);

        // 4. Reset attempts...
        // Clears old failed attempts, so that user gets fresh chances on new OTP...
        redisOtpService.resetAttempts(phone);

        // 5. Build and publish outbox event...
        OtpEvent otpEvent = otpEventBuilder.buildOtpEvent(phone, otpId, OtpType.VERIFY);
        OutboxEvent outboxOtpEvent = outboxEventService.buildOutboxEvent(otpEvent, OutboxEventType.OTP_NOTIFICATION);
        outboxEventService.publishOutBoxEvent(outboxOtpEvent);
    }

    public void verifyOtpAndActivateUser(String phone, String inputOtp) {

        User user = userService.findUserByPhone(phone);
        phoneVerificationCheck(user.getPhoneVerificationStatus());

        // 1. Check if blocked...
        if (redisOtpService.isBlocked(phone)) {
            throw new OtpBlockedException("Too many failed attempts. Try later.");
        }

        // 2️. Fetch OTP...
        String otpId = redisOtpService.getOtpIdByPhone(phone);

        if (otpId == null) {
            throw new OtpExpiredException("OTP expired or not found");
        }

        String storedOtp = redisOtpService.getOtpById(otpId);

        // 3️. Validate OTP...
        if (!storedOtp.equals(inputOtp)) {

            int attempts = redisOtpService.incrementAttempts(phone);

            log.warn("Invalid OTP attempt {} for {}", attempts, MaskingUtil.maskPhone(phone));

            throw new InvalidOtpException("Invalid OTP");
        }

        // success (so delete used otp and clear phone attempts)...
        redisOtpService.deleteOtpById(phone);
        redisOtpService.resetAttempts(phone);

        log.info("OTP verified successfully for {}", MaskingUtil.maskPhone(phone));

        // 4. Activate user...
        user.setPhoneVerificationStatus(PhoneVerificationStatus.VERIFIED);
        activateUserIfFullyVerified(user);
        userService.saveUser(user);

        log.info("Phone verified for user: {}", MaskingUtil.maskPhone(phone));

    }

}