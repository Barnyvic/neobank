package com.vaultpay.auth.service.impl;

import com.vaultpay.auth.service.AccessTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisAccessTokenStore implements AccessTokenStore {

    private static final String KEY_PREFIX = "auth:access:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void store(String jti, Long userId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + jti,
                userId.toString(),
                Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Long getUserId(String jti) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + jti);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void revoke(String jti) {
        redisTemplate.delete(KEY_PREFIX + jti);
    }
}
