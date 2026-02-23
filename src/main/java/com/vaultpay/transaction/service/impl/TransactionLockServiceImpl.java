package com.vaultpay.transaction.service.impl;

import com.vaultpay.transaction.service.TransactionLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TransactionLockServiceImpl implements TransactionLockService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final String KEY_PREFIX = "lock:txn:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean acquireLock(Long userId, Long walletId) {
        String key = KEY_PREFIX + userId + ":" + walletId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseLock(Long userId, Long walletId) {
        String key = KEY_PREFIX + userId + ":" + walletId;
        redisTemplate.delete(key);
    }
}
