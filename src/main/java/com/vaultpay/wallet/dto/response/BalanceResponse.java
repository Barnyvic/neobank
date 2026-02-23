package com.vaultpay.wallet.dto.response;

import com.vaultpay.wallet.entity.Wallet;

import java.math.BigDecimal;

public record BalanceResponse(
        Long walletId,
        String walletNumber,
        BigDecimal availableBalance,
        String currency
) {
    public static BalanceResponse from(Wallet wallet) {
        return new BalanceResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getBalance(),
                wallet.getCurrency().name()
        );
    }
}
