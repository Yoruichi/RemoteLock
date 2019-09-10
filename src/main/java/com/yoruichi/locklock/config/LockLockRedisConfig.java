package com.yoruichi.locklock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

@Configuration
public class LockLockRedisConfig {
    private Logger logger = LoggerFactory.getLogger(LockLockRedisConfig.class);

    @Bean(value = "lock")
    public RedisTemplate<String, String> redisTemplate(
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port,
            @Value("${spring.redis.database.locklock:0}") int db,
            @Value("${spring.redis.password:}") String password
    ) {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(db);
        if (Objects.nonNull(password) && password.trim().length() > 0) {
            configuration.setPassword(password);
        }

        RedisTemplate template = new StringRedisTemplate(new JedisConnectionFactory(configuration));
        logger.info("Create redis template with url {}", ((JedisConnectionFactory) template.getConnectionFactory()).getHostName());
        return template;
    }

}
