package com.vaultpay.auth.service.impl;

import com.vaultpay.auth.config.JwtKeyLoader;
import com.vaultpay.auth.config.JwtProperties;
import com.vaultpay.auth.service.AccessTokenStore;
import com.vaultpay.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final AccessTokenStore accessTokenStore;
    private final long accessTokenExpirationMs;
    private final String issuer;

    public JwtServiceImpl(
            JwtKeyLoader keyLoader,
            JwtProperties properties,
            AccessTokenStore accessTokenStore) {
        this.privateKey = keyLoader.loadPrivateKey();
        this.publicKey = keyLoader.loadPublicKey();
        this.accessTokenStore = accessTokenStore;
        this.accessTokenExpirationMs = properties.getAccessTokenExpirationMs();
        this.issuer = properties.getIssuer();
    }

    @Override
    public String generateAccessToken(Long userId) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);
        long ttlSeconds = accessTokenExpirationMs / 1000;

        accessTokenStore.store(jti, userId, ttlSeconds);

        return Jwts.builder()
                .id(jti)
                .issuer(issuer)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @Override
    public String extractJti(String token) {
        try {
            return extractClaims(token).getId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            String tokenType = claims.get(CLAIM_TYPE, String.class);
            return TOKEN_TYPE_ACCESS.equals(tokenType)
                    && issuer.equals(claims.getIssuer());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getRemainingValiditySeconds(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
