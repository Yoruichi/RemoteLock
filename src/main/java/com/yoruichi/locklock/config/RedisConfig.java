package com.yoruichi.locklock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {
    private Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Bean(value = "lock")
    public RedisTemplate<String, String> redisTemplate(
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port,
            @Value("${spring.redis.lock.database:0}") int db
    ) {
        JedisConnectionFactory jedis = new JedisConnectionFactory();
        jedis.setHostName(host);
        jedis.setPort(port);
        jedis.setDatabase(db);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(8);
        poolConfig.setMaxWaitMillis(-1);
        poolConfig.setTestOnBorrow(true);

        jedis.setPoolConfig(poolConfig);
        jedis.afterPropertiesSet();
        RedisTemplate<String, String> template = new StringRedisTemplate();
        template.setConnectionFactory(jedis);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        logger.info("Create redis template with url {}",
                ((JedisConnectionFactory) template.getConnectionFactory()).getHostName());
        return template;
    }

}
