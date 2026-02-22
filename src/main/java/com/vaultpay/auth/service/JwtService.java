package com.vaultpay.auth.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    String generateAccessToken(UserDetails userDetails);

    String extractUsername(String token);

    String extractJti(String token);

    boolean isTokenValid(String token, UserDetails userDetails);

    long getRemainingValiditySeconds(String token);
}
