package com.vaultpay.fraud.rule;

import com.vaultpay.fraud.config.FraudProperties;
import com.vaultpay.fraud.dto.FraudCheckContext;
import com.vaultpay.fraud.enums.FraudOperationType;
import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LargeWithdrawalRule implements FraudRule {

    private final FraudProperties properties;

    @Override
    public Optional<FraudRuleResult> evaluate(FraudCheckContext context) {
        if (context.operationType() != FraudOperationType.WITHDRAWAL) {
            return Optional.empty();
        }

        if (context.amount().compareTo(properties.getLargeWithdrawalReviewThreshold()) > 0) {
            return Optional.of(new FraudRuleResult(
                    FraudRuleType.LARGE_WITHDRAWAL,
                    FraudSeverity.REVIEW,
                    "Withdrawal amount exceeds review threshold"));
        }
        return Optional.empty();
    }
}
