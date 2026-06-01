package com.vaultpay.fraud.dto;

import com.vaultpay.fraud.enums.FraudOperationType;

import java.math.BigDecimal;

public record FraudCheckContext(
        Long userId,
        FraudOperationType operationType,
        BigDecimal amount,
        String currency,
        Long walletId,
        String recipientWalletNumber,
        String bankCode,
        String accountNumber) {

    public static FraudCheckContext forTransfer(
            Long userId, BigDecimal amount, String currency, String recipientWalletNumber) {
        return new FraudCheckContext(
                userId, FraudOperationType.TRANSFER, amount, currency, null, recipientWalletNumber, null, null);
    }

    public static FraudCheckContext forWithdrawal(
            Long userId, Long walletId, BigDecimal amount, String currency, String bankCode, String accountNumber) {
        return new FraudCheckContext(
                userId, FraudOperationType.WITHDRAWAL, amount, currency, walletId, null, bankCode, accountNumber);
    }

    public static FraudCheckContext forDeposit(Long userId, Long walletId, BigDecimal amount, String currency) {
        return new FraudCheckContext(
                userId, FraudOperationType.DEPOSIT, amount, currency, walletId, null, null, null);
    }
}
