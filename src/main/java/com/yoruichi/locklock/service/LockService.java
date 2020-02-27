package com.yoruichi.locklock.service;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.support.AsyncPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConverters;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by yoruichi on 17/8/21.
 */
@Service
public class LockService {
    @Autowired
    @Qualifier("lock")
    private RedisTemplate<String, String> redisTemplate;

    private Logger logger = LoggerFactory.getLogger(LockService.class);

    private static final String PREFIX_LOCK_KEY = "LOCK_";

    public void releaseAllLock() {
        Set<String> locks = redisTemplate.keys(PREFIX_LOCK_KEY + "*");
        locks.stream().forEach(k -> redisTemplate.delete(k));
    }

    public boolean getLockIfAbsent(final String key, final String value, long expire, TimeUnit timeUnit) {
        if (expire > 0) {
            return redisTemplate.opsForValue().setIfAbsent(PREFIX_LOCK_KEY + key, value, expire, timeUnit);
        } else {
            return redisTemplate.opsForValue().setIfAbsent(PREFIX_LOCK_KEY + key, value);
        }
    }

    /**
     * Get lock for key.Quick failed.
     *
     * @param key
     * @param value
     * @return
     */
    public boolean getLockIfAbsent(final String key, final String value) {
        return getLockIfAbsent(key, value, -1, null);
    }

    /**
     * Get lock for key with timeout.If set @param timeout -1, means it will be block to wait for getting the lock.
     * And if set @param lockExpireTime -1 means the lock will be never expired.
     *
     * @param key
     * @param value
     * @param waitTimeout
     * @param waitTimeUnit
     * @param lockExpireTime
     * @param lockExpireTimeUnit
     * @return
     */
    public boolean getLockIfAbsent(final String key, final String value, long waitTimeout, TimeUnit waitTimeUnit, long lockExpireTime,
            TimeUnit lockExpireTimeUnit) throws TimeoutException, InterruptedException, ExecutionException {
        CompletableFuture<Boolean> cf = CompletableFuture.supplyAsync(() -> {
            String realKey = PREFIX_LOCK_KEY + key;
            while (!Thread.currentThread().isInterrupted()) {
                boolean gotLock;
                if (lockExpireTime > 0) {
                    gotLock = redisTemplate.opsForValue().setIfAbsent(realKey, value, lockExpireTime, lockExpireTimeUnit);
                } else {
                    gotLock = redisTemplate.opsForValue().setIfAbsent(realKey, value);
                }
                if (!gotLock) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                    }
                } else {
                    return true;
                }
            }
            logger.warn("Thread {} was interrupted.", Thread.currentThread().getName());
            return false;
        });
        try {
            if (waitTimeout < 0) {
                return cf.get();
            } else {
                return cf.get(waitTimeout, waitTimeUnit);
            }
        } catch (TimeoutException te) {
            if (cf.cancel(true)) {
                logger.warn("Timeout to get lock with key {} and value {}", key, value);
            }
            return false;
        }
    }

    public boolean returnLock(final String key, final String value) {
        String realKey = PREFIX_LOCK_KEY + key;
        if (value != null && value.equals(redisTemplate.opsForValue().get(realKey))) {
            redisTemplate.delete(realKey);
            return true;
        }
        return false;
    }
}
