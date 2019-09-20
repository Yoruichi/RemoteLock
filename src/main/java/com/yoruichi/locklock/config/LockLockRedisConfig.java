package com.yoruichi.locklock.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

@Configuration
public class LockLockRedisConfig {
    @Bean(value = "lock")
    public RedisTemplate<String, String> redisTemplate(
            @Value("${spring.redis.host.locklock:localhost}") String host,
            @Value("${spring.redis.port.locklock:6379}") int port,
            @Value("${spring.redis.database.locklock:0}") int db,
            @Value("${spring.redis.password.locklock:}") String password,
            @Value("${spring.redis.pool.max-active.locklock:64}") int maxActive,
            @Value("${spring.redis.pool.max-wait.locklock:-1}") int maxWait,
            @Value("${spring.redis.pool.max-idle.locklock:64}") int maxIdle,
            @Value("${spring.redis.pool.min-idle.locklock:32}") int minIdle
    ) {

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxWaitMillis(maxWait);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(60000L);
        poolConfig.setTimeBetweenEvictionRunsMillis(30000L);
        poolConfig.setNumTestsPerEvictionRun(-1);
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(db);
        if (Objects.nonNull(password) && password.trim().length() > 0) {
            configuration.setPassword(password);
        }

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(configuration,
                LettucePoolingClientConfiguration.builder().poolConfig(poolConfig).build());
        lettuceConnectionFactory.afterPropertiesSet();
        RedisTemplate template = new StringRedisTemplate(lettuceConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }

}
