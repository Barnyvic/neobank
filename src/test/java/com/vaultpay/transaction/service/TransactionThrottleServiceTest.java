package com.vaultpay.transaction.service;

import com.vaultpay.transaction.service.impl.TransactionThrottleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionThrottleService Tests")
class TransactionThrottleServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private TransactionThrottleServiceImpl throttleService;

    @Test
    @DisplayName("should allow first transaction and mark it")
    void shouldAllowFirstTransaction() {
        String expectedKey = "throttle:txn:1:1000:9999999999";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(expectedKey), eq("1"), eq(Duration.ofSeconds(30))))
                .thenReturn(true);

        boolean allowed = throttleService.checkAndMark(1L, BigDecimal.valueOf(1000), "9999999999");

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("should reject duplicate transaction within throttle window")
    void shouldRejectDuplicate() {
        String expectedKey = "throttle:txn:1:1000:9999999999";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(expectedKey), eq("1"), eq(Duration.ofSeconds(30))))
                .thenReturn(false);

        boolean allowed = throttleService.checkAndMark(1L, BigDecimal.valueOf(1000), "9999999999");

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("should distinguish different amounts and destinations")
    void shouldDistinguishDifferentTransactions() {
        String key1 = "throttle:txn:1:1000:9999999999";
        String key2 = "throttle:txn:1:2000:8888888888";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(key1), eq("1"), eq(Duration.ofSeconds(30)))).thenReturn(true);
        when(valueOps.setIfAbsent(eq(key2), eq("1"), eq(Duration.ofSeconds(30)))).thenReturn(true);

        assertThat(throttleService.checkAndMark(1L, BigDecimal.valueOf(1000), "9999999999")).isTrue();
        assertThat(throttleService.checkAndMark(1L, BigDecimal.valueOf(2000), "8888888888")).isTrue();
    }
}
