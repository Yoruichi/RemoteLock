package com.yoruichi.locklock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Objects;

@Configuration
public class LockLockRedisConfig {
    private Logger logger = LoggerFactory.getLogger(LockLockRedisConfig.class);

    @Bean(value = "lock")
    public RedisTemplate<String, String> redisTemplate(
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port,
            @Value("${spring.redis.database.locklock:0}") int db,
            @Value("${spring.redis.password:}") String password,
            @Value("${spring.redis.active.locklock:64}") int maxActive,
            @Value("${spring.redis.wait.locklock:64}") int maxWait,
            @Value("${spring.redis.idle.max.locklock:64}") int maxIdle,
            @Value("${spring.redis.idle.min.locklock:32}") int minIdle
    ) {

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxActive);
        jedisPoolConfig.setMaxWaitMillis(maxWait);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(db);
        if (Objects.nonNull(password) && password.trim().length() > 0) {
            configuration.setPassword(password);
        }
        JedisConnectionFactory factory = new JedisConnectionFactory(configuration);
        factory.setPoolConfig(jedisPoolConfig);
        factory.afterPropertiesSet();
        RedisTemplate template = new StringRedisTemplate(factory);
        logger.info("Create redis template with url {}", ((JedisConnectionFactory) template.getConnectionFactory()).getHostName());
        return template;
    }

}
