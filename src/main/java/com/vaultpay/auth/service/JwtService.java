package com.vaultpay.auth.service;

public interface JwtService {
    String generateAccessToken(Long userId);

    String extractJti(String token);

    boolean isTokenValid(String token);

    long getRemainingValiditySeconds(String token);
}
