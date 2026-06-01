package com.vaultpay.fraud.rule;

import com.vaultpay.fraud.dto.FraudCheckContext;

import java.util.Optional;

public interface FraudRule {

    Optional<FraudRuleResult> evaluate(FraudCheckContext context);
}
