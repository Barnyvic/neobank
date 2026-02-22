package com.vaultpay.auth.service;

public interface LoginAttemptService {

    void recordFailure(String email);

    void recordSuccess(String email);

    boolean isLocked(String email);
}
