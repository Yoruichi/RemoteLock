package com.yoruichi.locklock;

import com.yoruichi.locklock.annotations.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    private Logger logger = LoggerFactory.getLogger(TestService.class);

    private int num;

    @Autowired
    private RedisTemplate redisTemplate;

    @Synchronized(name = "test", waitTimeInMilliSeconds = "${wait:1000}", expiredTimeInMilliSeconds = "900")
    public void incrementOne() {
        num++;
        redisTemplate.opsForValue().increment("lock_testing");
    }

    public int getNum() {
        return num;
    }
}
