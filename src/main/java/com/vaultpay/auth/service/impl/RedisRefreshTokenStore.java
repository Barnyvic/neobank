package com.vaultpay.auth.service.impl;

import com.vaultpay.auth.service.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.refresh-token.ttl-hours:168}") 
    private long refreshTokenTtlHours;

    @Override
    public String createToken(Long userId) {
        String token = generateToken();
        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(
                key,
                userId.toString(),
                Duration.ofHours(refreshTokenTtlHours)
        );
        return token;
    }

    @Override
    public Long getUserId(String refreshToken) {
        String key = KEY_PREFIX + refreshToken;
        String value = redisTemplate.opsForValue().get(key);
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
    public void revoke(String refreshToken) {
        String key = KEY_PREFIX + refreshToken;
        redisTemplate.delete(key);
    }

    @Override
    public void revokeAllForUser(Long userId) {
        String pattern = KEY_PREFIX + "*";
        redisTemplate.keys(pattern).forEach(key -> {
            String value = redisTemplate.opsForValue().get(key);
            if (userId.toString().equals(value)) {
                redisTemplate.delete(key);
            }
        });
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

