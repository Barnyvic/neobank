package com.vaultpay.common.config;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

@Configuration
public class RedisConfig {

    @Bean
    public RedisClient bucket4jRedisClient(RedisProperties redisProperties) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort());

        String password = redisProperties.getPassword();
        if (password != null && !password.isBlank()) {
            builder.withPassword(password.toCharArray());
        }

        return RedisClient.create(builder.build());
    }

    @Bean
    public LettuceBasedProxyManager bucket4jProxyManager(RedisClient bucket4jRedisClient) {
        return LettuceBasedProxyManager.builderFor(bucket4jRedisClient).build();
    }
}

