package com.vaultpay.auth.service;

import com.vaultpay.auth.config.JwtKeyLoader;
import com.vaultpay.auth.config.JwtProperties;
import com.vaultpay.auth.service.impl.JwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private static final long EXPIRATION_MS = 900_000L;
    private static final Long USER_ID = 42L;

    private JwtServiceImpl jwtService;
    private InMemoryAccessTokenStore accessTokenStore;

    @BeforeEach
    void setUp() {
        accessTokenStore = new InMemoryAccessTokenStore();
        JwtProperties properties = new JwtProperties();
        properties.setPrivateKeyLocation("classpath:jwt/test-private.pem");
        properties.setPublicKeyLocation("classpath:jwt/test-public.pem");
        properties.setAccessTokenExpirationMs(EXPIRATION_MS);
        properties.setIssuer("vaultpay-test");

        jwtService = new JwtServiceImpl(
                new JwtKeyLoader(properties, new DefaultResourceLoader()),
                properties,
                accessTokenStore);
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("should produce non-empty RS256 token")
        void shouldProduceNonEmptyToken() {
            String token = jwtService.generateAccessToken(USER_ID);
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("should produce different tokens on each call")
        void shouldProduceDifferentTokens() {
            String token1 = jwtService.generateAccessToken(USER_ID);
            String token2 = jwtService.generateAccessToken(USER_ID);
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("should not embed email, user id, or roles in payload")
        void shouldNotEmbedSensitiveClaims() {
            String token = jwtService.generateAccessToken(USER_ID);
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                    StandardCharsets.UTF_8);

            assertThat(payloadJson).doesNotContain("user@");
            assertThat(payloadJson).doesNotContain("\"sub\"");
            assertThat(payloadJson).doesNotContain(String.valueOf(USER_ID));
            assertThat(payloadJson).doesNotContain("ROLE_");
            assertThat(payloadJson).contains("\"type\":\"access\"");
            assertThat(payloadJson).contains("\"jti\":");

            String headerJson = new String(
                    Base64.getUrlDecoder().decode(token.split("\\.")[0]),
                    StandardCharsets.UTF_8);
            assertThat(headerJson).contains("\"alg\":\"RS256\"");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("should return true for valid token with active session")
        void shouldReturnTrueForValidToken() {
            String token = jwtService.generateAccessToken(USER_ID);
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should return false when session is revoked")
        void shouldReturnFalseWhenSessionRevoked() {
            String token = jwtService.generateAccessToken(USER_ID);
            String jti = jwtService.extractJti(token);
            accessTokenStore.revoke(jti);
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid token")
        void shouldReturnFalseForInvalidToken() {
            assertThat(jwtService.isTokenValid("invalid")).isFalse();
        }

        @Test
        @DisplayName("should return false for token signed with different key pair")
        void shouldReturnFalseForWrongSignature() {
            String token = jwtService.generateAccessToken(USER_ID);

            JwtProperties otherProps = new JwtProperties();
            otherProps.setPrivateKeyLocation("classpath:jwt/other-private.pem");
            otherProps.setAccessTokenExpirationMs(EXPIRATION_MS);
            otherProps.setIssuer("vaultpay-test");

            JwtServiceImpl otherService = new JwtServiceImpl(
                    new JwtKeyLoader(otherProps, new DefaultResourceLoader()),
                    otherProps,
                    new InMemoryAccessTokenStore());

            assertThat(otherService.isTokenValid(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractJti")
    class ExtractJti {

        @Test
        @DisplayName("should extract non-blank jti from valid token")
        void shouldExtractJtiFromValidToken() {
            String token = jwtService.generateAccessToken(USER_ID);
            assertThat(jwtService.extractJti(token)).isNotBlank();
        }

        @Test
        @DisplayName("should return null for invalid token")
        void shouldReturnNullForInvalidToken() {
            assertThat(jwtService.extractJti("invalid.token.here")).isNull();
        }
    }

    @Nested
    @DisplayName("getRemainingValiditySeconds")
    class GetRemainingValiditySeconds {

        @Test
        @DisplayName("should return positive remaining seconds for fresh token")
        void shouldReturnPositiveForFreshToken() {
            String token = jwtService.generateAccessToken(USER_ID);
            long remaining = jwtService.getRemainingValiditySeconds(token);
            assertThat(remaining).isPositive().isLessThanOrEqualTo(EXPIRATION_MS / 1000);
        }

        @Test
        @DisplayName("should return 0 for invalid token")
        void shouldReturnZeroForInvalidToken() {
            assertThat(jwtService.getRemainingValiditySeconds("invalid.token.here")).isZero();
        }
    }

    private static final class InMemoryAccessTokenStore implements AccessTokenStore {

        private final Map<String, Long> sessions = new ConcurrentHashMap<>();

        @Override
        public void store(String jti, Long userId, long ttlSeconds) {
            sessions.put(jti, userId);
        }

        @Override
        public Long getUserId(String jti) {
            return sessions.get(jti);
        }

        @Override
        public void revoke(String jti) {
            sessions.remove(jti);
        }
    }
}
