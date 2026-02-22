package com.vaultpay.auth.service.impl;

import com.vaultpay.auth.service.LoginAttemptService;
import com.vaultpay.common.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final String KEY_PREFIX = "auth:login-attempts:";

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties securityProperties;

    @Override
    public void recordFailure(String email) {
        String key = KEY_PREFIX + email.toLowerCase();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, securityProperties.getLogin().getLockoutMinutes(), TimeUnit.MINUTES);
        }
    }

    @Override
    public void recordSuccess(String email) {
        redisTemplate.delete(KEY_PREFIX + email.toLowerCase());
    }

    @Override
    public boolean isLocked(String email) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + email.toLowerCase());
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= securityProperties.getLogin().getMaxAttempts();
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
