package com.vaultpay.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private boolean enabled = true;

    private String allowedOrigins;

    private List<String> allowedMethods = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private List<String> allowedHeaders = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Idempotency-Key",
            "X-Request-Id");

    private List<String> exposedHeaders = List.of("Retry-After");

    private boolean allowCredentials = true;

    private long maxAgeSeconds = 3600;

    public List<String> resolvedAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
