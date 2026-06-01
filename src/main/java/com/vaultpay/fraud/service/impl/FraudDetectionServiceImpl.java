package com.vaultpay.fraud.service.impl;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.fraud.config.FraudProperties;
import com.vaultpay.fraud.dto.FraudCheckContext;
import com.vaultpay.fraud.entity.FraudAlert;
import com.vaultpay.fraud.enums.FraudAlertStatus;
import com.vaultpay.fraud.enums.FraudSeverity;
import com.vaultpay.fraud.repository.FraudAlertRepository;
import com.vaultpay.fraud.rule.FraudRule;
import com.vaultpay.fraud.rule.FraudRuleResult;
import com.vaultpay.fraud.service.FraudDetectionService;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final FraudProperties properties;
    private final List<FraudRule> rules;
    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void evaluateOrThrow(FraudCheckContext context) {
        if (!properties.isEnabled()) {
            return;
        }

        List<FraudRuleResult> triggered = new ArrayList<>();
        for (FraudRule rule : rules) {
            rule.evaluate(context).ifPresent(triggered::add);
        }

        if (triggered.isEmpty()) {
            return;
        }

        User user = userRepository.findById(context.userId())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND));

        for (FraudRuleResult result : triggered) {
            if (result.severity() == FraudSeverity.REVIEW) {
                persistAlert(user, context, result);
            }
        }

        FraudRuleResult blocking = triggered.stream()
                .filter(r -> r.severity() == FraudSeverity.BLOCK)
                .findFirst()
                .orElse(null);

        if (blocking != null) {
            persistAlert(user, context, blocking);
            log.warn("Fraud block: userId={}, rule={}, op={}", context.userId(), blocking.ruleType(), context.operationType());
            throw new BusinessException(blocking.message(), HttpStatus.FORBIDDEN, ErrorCode.FRAUD_DETECTED);
        }

        if (triggered.stream().anyMatch(r -> r.severity() == FraudSeverity.REVIEW)) {
            log.info("Fraud review flags for userId={}, op={}, count={}",
                    context.userId(), context.operationType(), triggered.size());
        }
    }

    private void persistAlert(User user, FraudCheckContext context, FraudRuleResult result) {
        FraudAlert alert = FraudAlert.builder()
                .user(user)
                .ruleType(result.ruleType())
                .severity(result.severity())
                .status(FraudAlertStatus.OPEN)
                .operationType(context.operationType())
                .amount(context.amount())
                .currency(context.currency())
                .recipientWalletNumber(context.recipientWalletNumber())
                .walletId(context.walletId())
                .message(result.message())
                .build();
        fraudAlertRepository.save(alert);
    }
}
