package com.vaultpay.fraud.rule;

import com.vaultpay.fraud.config.FraudProperties;
import com.vaultpay.fraud.dto.FraudCheckContext;
import com.vaultpay.fraud.enums.FraudOperationType;
import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;
import com.vaultpay.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NewRecipientRule implements FraudRule {

    private final FraudProperties properties;
    private final TransactionRepository transactionRepository;

    @Override
    public Optional<FraudRuleResult> evaluate(FraudCheckContext context) {
        if (context.operationType() != FraudOperationType.TRANSFER
                || context.recipientWalletNumber() == null
                || context.recipientWalletNumber().isBlank()) {
            return Optional.empty();
        }

        if (context.amount().compareTo(properties.getNewRecipientReviewThreshold()) <= 0) {
            return Optional.empty();
        }

        long priorTransfers = transactionRepository.countCompletedTransfersToRecipient(
                context.userId(), context.recipientWalletNumber());
        if (priorTransfers > 0) {
            return Optional.empty();
        }

        return Optional.of(new FraudRuleResult(
                FraudRuleType.NEW_RECIPIENT,
                FraudSeverity.REVIEW,
                "Large transfer to a recipient with no prior completed transfers"));
    }
}
