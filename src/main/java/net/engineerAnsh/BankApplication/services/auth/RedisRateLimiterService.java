package net.engineerAnsh.BankApplication.services.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisRateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${redis.auth.rate-limit.max-requests}")
    private int maxRequests;

    @Value("${redis.auth.rate-limit.window-seconds}")
    private int windowSeconds;

    // It limits a user (or IP/email combo) to: 5 requests per 1 minute, After that -> request is rejected.
    public boolean isAllowed(String key) {

        String redisKey = "rate_limit:" + key;

        // It helps us: If key does NOT exist -> creates it with value = 1
        // If exists -> increments atomically
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        // When the first request happens, Then we set : key expiry of 60 seconds...
        // So after 1 minute: Redis automatically deletes the key (This resets the counter).
        if (currentCount!= null && currentCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        // If count > 5 → return false...
        return (currentCount!= null && currentCount <= maxRequests);
    }
}
