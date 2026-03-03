package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisRateLimiterService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    // It limits a user (or IP/email combo) to: 5 requests per 1 minute, After that -> request is rejected.
    public boolean isAllowed(String key) {

        String redisKey = "rate_limit:" + key;

        // It helps us: If key does NOT exist -> creates it with value = 1
        // If exists -> increments atomically
        Long currentCount = redisTemplate.opsForValue()
                .increment(redisKey);

        // When the first request happens, Then we set : key expiry of 60 seconds...
        // So after 1 minute: Redis automatically deletes the key (This resets the counter).
        if (currentCount!= null && currentCount == 1) {
            redisTemplate.expire(redisKey, WINDOW);
        }

        // If count > 5 → return false...
        return (currentCount!= null && currentCount <= MAX_REQUESTS);
    }
}
