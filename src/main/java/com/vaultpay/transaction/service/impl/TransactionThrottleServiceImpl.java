package com.vaultpay.transaction.service.impl;

import com.vaultpay.transaction.service.TransactionThrottleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TransactionThrottleServiceImpl implements TransactionThrottleService {

    private static final Duration THROTTLE_WINDOW = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "throttle:txn:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean checkAndMark(Long userId, BigDecimal amount, String destWalletNumber) {
        String key = KEY_PREFIX + userId + ":" + amount.stripTrailingZeros().toPlainString() + ":" + destWalletNumber;
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "1", THROTTLE_WINDOW);
        return Boolean.TRUE.equals(wasAbsent);
    }
}
