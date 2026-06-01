package com.vaultpay.fraud.rule;

import com.vaultpay.fraud.config.FraudProperties;
import com.vaultpay.fraud.dto.FraudCheckContext;
import com.vaultpay.fraud.enums.FraudOperationType;
import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;
import com.vaultpay.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static com.vaultpay.common.util.MoneyUtil.scale;

@Component
@RequiredArgsConstructor
public class DailyOutboundLimitRule implements FraudRule {

    private static final Set<FraudOperationType> OUTBOUND = Set.of(
            FraudOperationType.TRANSFER, FraudOperationType.WITHDRAWAL);

    private final FraudProperties properties;
    private final TransactionRepository transactionRepository;

    @Override
    public Optional<FraudRuleResult> evaluate(FraudCheckContext context) {
        if (!OUTBOUND.contains(context.operationType())) {
            return Optional.empty();
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        BigDecimal spentToday = scale(transactionRepository.sumOutboundAmountSince(context.userId(), since));
        BigDecimal projectedTotal = spentToday.add(context.amount());

        if (projectedTotal.compareTo(properties.getMaxDailyOutbound()) > 0) {
            return Optional.of(new FraudRuleResult(
                    FraudRuleType.DAILY_OUTBOUND_LIMIT,
                    FraudSeverity.BLOCK,
                    "Daily outbound limit exceeded"));
        }
        return Optional.empty();
    }
}
