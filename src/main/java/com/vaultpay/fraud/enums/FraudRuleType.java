package com.vaultpay.fraud.enums;

public enum FraudRuleType {
    SINGLE_TRANSACTION_LIMIT,
    DAILY_OUTBOUND_LIMIT,
    HOURLY_VELOCITY,
    DAILY_VELOCITY,
    NEW_RECIPIENT,
    LARGE_WITHDRAWAL
}
