package com.vaultpay.fraud.rule;

import com.vaultpay.fraud.config.FraudProperties;
import com.vaultpay.fraud.dto.FraudCheckContext;
import com.vaultpay.fraud.enums.FraudOperationType;
import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;
import com.vaultpay.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TransactionVelocityRule implements FraudRule {

    private static final Set<FraudOperationType> OUTBOUND = Set.of(
            FraudOperationType.TRANSFER, FraudOperationType.WITHDRAWAL);

    private final FraudProperties properties;
    private final TransactionRepository transactionRepository;

    @Override
    public Optional<FraudRuleResult> evaluate(FraudCheckContext context) {
        if (!OUTBOUND.contains(context.operationType())) {
            return Optional.empty();
        }

        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);
        long hourlyCount = transactionRepository.countOutboundTransactionsSince(context.userId(), hourAgo);
        if (hourlyCount >= properties.getMaxTransactionsPerHour()) {
            return Optional.of(new FraudRuleResult(
                    FraudRuleType.HOURLY_VELOCITY,
                    FraudSeverity.BLOCK,
                    "Too many transactions in the last hour"));
        }

        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        long dailyCount = transactionRepository.countOutboundTransactionsSince(context.userId(), dayAgo);
        if (dailyCount >= properties.getMaxTransactionsPerDay()) {
            return Optional.of(new FraudRuleResult(
                    FraudRuleType.DAILY_VELOCITY,
                    FraudSeverity.BLOCK,
                    "Too many transactions in the last 24 hours"));
        }

        return Optional.empty();
    }
}
