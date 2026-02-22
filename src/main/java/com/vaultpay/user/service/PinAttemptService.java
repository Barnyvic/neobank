package com.vaultpay.user.service;

public interface PinAttemptService {

    void recordFailure(Long userId);

    void recordSuccess(Long userId);

    boolean isLocked(Long userId);
}
