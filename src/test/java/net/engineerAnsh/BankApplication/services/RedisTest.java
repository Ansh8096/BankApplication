package net.engineerAnsh.BankApplication.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisTest {

    @Autowired
    private RedisTemplate redisTemplate;


    @Test
    void Test1(){ // This is just a test to check whether a connection with 'redis' is made successfully or not.
        redisTemplate.opsForValue().set("email","anshv8096@gmail.com");
        String value = (String)redisTemplate.opsForValue().get("email");
        int a = 109;
    }
}
