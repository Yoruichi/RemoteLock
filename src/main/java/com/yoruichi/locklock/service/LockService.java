package com.yoruichi.locklock.service;

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

    @Autowired
    private GenericObjectPool<StatefulRedisConnection<String, String>> lettucePool;

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

    public boolean getLockIfAbsent(final String key, final String value, long waitTimeout, TimeUnit waitTimeUnit, long lockExpireTime,
            TimeUnit lockExpireTimeUnit) throws InterruptedException, TimeoutException {
        final String realKey = PREFIX_LOCK_KEY + key;
        try (StatefulRedisConnection<String, String> conn = lettucePool.borrowObject()) {
            RedisAsyncCommands<String, String> rac = conn.async();
            if (lockExpireTime > 0) {
                String res = rac.psetex(realKey, lockExpireTimeUnit.toMillis(lockExpireTime), value).get(waitTimeout, waitTimeUnit);
                return LettuceConverters.stringToBoolean(res);
            } else {
                return rac.setex(realKey, -1, value).thenApply(LettuceConverters::stringToBoolean).toCompletableFuture().complete(false);
            }
        } catch (InterruptedException ie) {
            throw ie;
        } catch (TimeoutException te) {
            throw te;
        } catch (Exception e) {
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
