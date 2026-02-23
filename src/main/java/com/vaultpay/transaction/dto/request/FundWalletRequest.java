package com.vaultpay.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FundWalletRequest(
        @NotNull(message = "Wallet ID is required")
        Long walletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "100.00", message = "Minimum funding amount is 100.00")
        BigDecimal amount,

        String idempotencyKey
) {}
