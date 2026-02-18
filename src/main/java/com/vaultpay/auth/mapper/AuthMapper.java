package com.vaultpay.auth.mapper;

import com.vaultpay.auth.dto.response.AuthResponse;

public final class AuthMapper {

    private static final String TOKEN_TYPE = "Bearer";

    private AuthMapper() {
    }

    public static AuthResponse toResponse(String accessToken, String refreshToken, long expiresInSeconds) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(expiresInSeconds)
                .build();
    }
}
