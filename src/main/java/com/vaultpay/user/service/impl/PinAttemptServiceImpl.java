package com.vaultpay.user.service.impl;

import com.vaultpay.common.config.SecurityProperties;
import com.vaultpay.user.service.PinAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PinAttemptServiceImpl implements PinAttemptService {

    private static final String KEY_PREFIX = "auth:pin-attempts:";

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties securityProperties;

    @Override
    public void recordFailure(Long userId) {
        String key = KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, securityProperties.getPin().getLockoutMinutes(), TimeUnit.MINUTES);
        }
    }

    @Override
    public void recordSuccess(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    @Override
    public boolean isLocked(Long userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= securityProperties.getPin().getMaxAttempts();
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
