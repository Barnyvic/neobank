package com.vaultpay.paystack.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.paystack")
@Getter
@Setter
public class PaystackConfig {

    private String secretKey;
    private String baseUrl;
}
