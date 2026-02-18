package com.vaultpay.auth.service;

public interface RefreshTokenStore {

    /**
     * Create and persist a new refresh token for the given user id, returning the opaque token string.
     */
    String createToken(Long userId);

    /**
     * Resolve a refresh token back to a user id, or null if not found/expired.
     */
    Long getUserId(String refreshToken);

    /**
     * Revoke a single refresh token.
     */
    void revoke(String refreshToken);

    /**
     * Revoke all refresh tokens for a given user.
     */
    void revokeAllForUser(Long userId);
}

