package com.vaultpay.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String privateKey;

    private String publicKey;

    private String privateKeyLocation;

    private String publicKeyLocation;

    private long accessTokenExpirationMs = 900_000;

    private String issuer = "vaultpay";
}
