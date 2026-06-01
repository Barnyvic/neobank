package com.vaultpay.fraud.rule;

import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;

public record FraudRuleResult(FraudRuleType ruleType, FraudSeverity severity, String message) {
}
