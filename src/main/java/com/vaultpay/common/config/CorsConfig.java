package com.vaultpay.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        if (properties.isEnabled()) {
            config.setAllowedOrigins(properties.resolvedAllowedOrigins());
            config.setAllowedMethods(properties.getAllowedMethods());
            config.setAllowedHeaders(properties.getAllowedHeaders());
            config.setExposedHeaders(properties.getExposedHeaders());
            config.setAllowCredentials(properties.isAllowCredentials());
            config.setMaxAge(properties.getMaxAgeSeconds());
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
