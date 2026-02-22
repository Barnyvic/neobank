package com.vaultpay.auth.service;

public interface TokenBlacklistService {

    void blacklist(String jti, long ttlSeconds);

    boolean isBlacklisted(String jti);
}
