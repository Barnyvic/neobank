package com.vaultpay.fraud.rule;

import com.vaultpay.fraud.config.FraudProperties;
import com.vaultpay.fraud.dto.FraudCheckContext;
import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SingleTransactionLimitRule implements FraudRule {

    private final FraudProperties properties;

    @Override
    public Optional<FraudRuleResult> evaluate(FraudCheckContext context) {
        if (context.amount().compareTo(properties.getMaxSingleTransaction()) > 0) {
            return Optional.of(new FraudRuleResult(
                    FraudRuleType.SINGLE_TRANSACTION_LIMIT,
                    FraudSeverity.BLOCK,
                    "Transaction amount exceeds the maximum allowed per transaction"));
        }
        return Optional.empty();
    }
}
