package com.vaultpay.fraud.dto.response;

import com.vaultpay.fraud.entity.FraudAlert;
import com.vaultpay.fraud.enums.FraudAlertStatus;
import com.vaultpay.fraud.enums.FraudOperationType;
import com.vaultpay.fraud.enums.FraudRuleType;
import com.vaultpay.fraud.enums.FraudSeverity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FraudAlertResponse(
        Long id,
        Long userId,
        String userEmail,
        FraudRuleType ruleType,
        FraudSeverity severity,
        FraudAlertStatus status,
        FraudOperationType operationType,
        BigDecimal amount,
        String currency,
        String recipientWalletNumber,
        Long walletId,
        String transactionReference,
        String message,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String resolutionNote) {

    public static FraudAlertResponse from(FraudAlert alert) {
        return new FraudAlertResponse(
                alert.getId(),
                alert.getUser().getId(),
                alert.getUser().getEmail(),
                alert.getRuleType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getOperationType(),
                alert.getAmount(),
                alert.getCurrency(),
                alert.getRecipientWalletNumber(),
                alert.getWalletId(),
                alert.getTransactionReference(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getResolvedAt(),
                alert.getResolutionNote());
    }
}
