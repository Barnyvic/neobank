package com.vaultpay.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rateLimit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;
    private Rate publicRate = new Rate();
    private Rate auth = new Rate();
    private Rate login = new Rate();

    @Getter
    @Setter
    public static class Rate {
        private long requestsPerMinute = 60;
    }
}

