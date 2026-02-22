package com.vaultpay.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private Login login = new Login();
    private Pin pin = new Pin();

    @Data
    public static class Login {
        private int maxAttempts = 5;
        private long lockoutMinutes = 15;
    }

    @Data
    public static class Pin {
        private int maxAttempts = 5;
        private long lockoutMinutes = 30;
    }
}
