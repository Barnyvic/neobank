package com.vaultpay.auth.config;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtKeyLoader {

    private final JwtProperties properties;
    private final org.springframework.core.io.ResourceLoader resourceLoader;

    public JwtKeyLoader(JwtProperties properties, org.springframework.core.io.ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public RSAPrivateKey loadPrivateKey() {
        String pem = resolvePem(properties.getPrivateKey(), properties.getPrivateKeyLocation());
        return parsePrivateKey(pem);
    }

    public RSAPublicKey loadPublicKey() {
        if (StringUtils.hasText(properties.getPublicKey()) || StringUtils.hasText(properties.getPublicKeyLocation())) {
            String pem = resolvePem(properties.getPublicKey(), properties.getPublicKeyLocation());
            return parsePublicKey(pem);
        }
        return derivePublicKey(loadPrivateKey());
    }

    private String resolvePem(String inline, String location) {
        if (StringUtils.hasText(inline)) {
            return inline.replace("\\n", "\n");
        }
        if (StringUtils.hasText(location)) {
            try {
                Resource resource = resourceLoader.getResource(location);
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read JWT private key from " + location, e);
            }
        }
        throw new IllegalStateException(
                "JWT private key not configured. Set JWT_PRIVATE_KEY or JWT_PRIVATE_KEY_LOCATION");
    }

    static RSAPrivateKey parsePrivateKey(String pem) {
        try {
            byte[] encoded = decodePem(pem, "PRIVATE KEY");
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid JWT private key", e);
        }
    }

    static RSAPublicKey parsePublicKey(String pem) {
        try {
            byte[] encoded = decodePem(pem, "PUBLIC KEY");
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid JWT public key", e);
        }
    }

    static RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
        try {
            var privateSpec = KeyFactory.getInstance("RSA").getKeySpec(privateKey, java.security.spec.RSAPrivateCrtKeySpec.class);
            var publicSpec = new RSAPublicKeySpec(privateSpec.getModulus(), privateSpec.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(publicSpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Could not derive public key from private key", e);
        }
    }

    private static byte[] decodePem(String pem, String label) {
        String normalized = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }
}
