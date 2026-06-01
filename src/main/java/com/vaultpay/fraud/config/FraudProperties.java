package com.vaultpay.fraud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.fraud")
public class FraudProperties {

    private boolean enabled = true;

    private BigDecimal maxSingleTransaction = new BigDecimal("5000000");

    private BigDecimal maxDailyOutbound = new BigDecimal("10000000");

    private int maxTransactionsPerHour = 20;

    private int maxTransactionsPerDay = 50;

    private BigDecimal newRecipientReviewThreshold = new BigDecimal("500000");

    private BigDecimal largeWithdrawalReviewThreshold = new BigDecimal("1000000");
}
