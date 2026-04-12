package net.engineerAnsh.BankApplication.services.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisOtpService {

    private final StringRedisTemplate redisTemplate;

    @Value("${redis.otp.ttl-seconds}")
    private int otpTtlSeconds;

    @Value("${redis.otp.max-attempts}")
    private int maxAttempts;

    @Value("${redis.otp.attempt-window-seconds}")
    private int attemptWindowSeconds;

    // Generate Key
    private String otpKey(String phone) {
        return "otp:" + phone;
    }

    private String attemptKey(String phone) {
        return "otp:wrong-attempt:" + phone;
    }

    public void resetAttempts(String phone) {
        redisTemplate.delete(attemptKey(phone));
    }

    public long getOtpTtl(String phone) {
        Long ttl = redisTemplate.getExpire(otpKey(phone));
        return ttl != null ? ttl : -1;
    }

    // Track attempts
    public int incrementAttempts(String phone) {
        Long attempts = redisTemplate.opsForValue().increment(attemptKey(phone));

        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey(phone), Duration.ofSeconds(attemptWindowSeconds));
        }

        return attempts != null ? attempts.intValue() : 0;
    }

    // Check max attempts
    public boolean isBlocked(String phone) {
        String val = redisTemplate.opsForValue().get(attemptKey(phone));

        if (val == null) return false;
        try {
            return Integer.parseInt(val) >= maxAttempts;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getOtpById(String otpId) {
        return redisTemplate.opsForValue().get("otp:id:" + otpId);
    }

    public String getOtpIdByPhone(String phone) {
        return redisTemplate.opsForValue().get("otp:phone:" + phone);
    }

    public void deleteOtpById(String phone) {

        String otpId = getOtpIdByPhone(phone);

        if (otpId != null) {
            redisTemplate.delete("otp:id:" + otpId);
        }

        redisTemplate.delete("otp:phone:" + phone);
    }

    public String saveOtpWithId(String phone, String otp) {

        // Remove old OTP if exists...
        String oldOtpId = getOtpIdByPhone(phone);
        if (oldOtpId != null) {
            redisTemplate.delete("otp:id:" + oldOtpId);
        }

        String otpId = UUID.randomUUID().toString();

        // Store OTP by otpId
        String otpKey = "otp:id:" + otpId;
        redisTemplate.opsForValue()
                .set(otpKey, otp, Duration.ofSeconds(otpTtlSeconds));

        // Map phone → otpId (for verification later)
        String phoneKey = "otp:phone:" + phone;
        redisTemplate.opsForValue()
                .set(phoneKey, otpId, Duration.ofSeconds(otpTtlSeconds));

        return otpId;
    }

    public boolean otpExistsByPhone(String phone) {
        return redisTemplate.hasKey("otp:phone:" + phone);
    }

}