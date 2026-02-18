package com.vaultpay.auth.service;

public interface RefreshTokenStore {

    String createToken(Long userId);

    Long getUserId(String refreshToken);

    void revoke(String refreshToken);

    void revokeAllForUser(Long userId);
}

