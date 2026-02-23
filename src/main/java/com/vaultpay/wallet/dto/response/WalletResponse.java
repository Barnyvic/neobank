package com.vaultpay.wallet.dto.response;

import com.vaultpay.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        String walletNumber,
        String currency,
        BigDecimal balance,
        String status,
        LocalDateTime createdAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getCurrency().name(),
                wallet.getBalance(),
                wallet.getStatus().name(),
                wallet.getCreatedAt()
        );
    }
}
