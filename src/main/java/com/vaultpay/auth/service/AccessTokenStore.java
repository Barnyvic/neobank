package com.vaultpay.auth.service;

public interface AccessTokenStore {

    void store(String jti, Long userId, long ttlSeconds);

    Long getUserId(String jti);

    void revoke(String jti);
}
