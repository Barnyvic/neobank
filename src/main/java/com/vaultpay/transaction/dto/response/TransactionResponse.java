package com.vaultpay.transaction.dto.response;

import com.vaultpay.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String reference,
        String transactionType,
        String status,
        BigDecimal amount,
        String currency,
        String description,
        String sourceWalletNumber,
        String destWalletNumber,
        String paystackAuthorizationUrl,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getReference(),
                txn.getTransactionType().name(),
                txn.getStatus().name(),
                txn.getAmount(),
                txn.getCurrency(),
                txn.getDescription(),
                txn.getSourceWallet() != null ? txn.getSourceWallet().getWalletNumber() : null,
                txn.getDestWallet() != null ? txn.getDestWallet().getWalletNumber() : null,
                null,
                txn.getCreatedAt()
        );
    }

    public static TransactionResponse from(Transaction txn, String paystackAuthorizationUrl) {
        return new TransactionResponse(
                txn.getId(),
                txn.getReference(),
                txn.getTransactionType().name(),
                txn.getStatus().name(),
                txn.getAmount(),
                txn.getCurrency(),
                txn.getDescription(),
                txn.getSourceWallet() != null ? txn.getSourceWallet().getWalletNumber() : null,
                txn.getDestWallet() != null ? txn.getDestWallet().getWalletNumber() : null,
                paystackAuthorizationUrl,
                txn.getCreatedAt()
        );
    }
}
