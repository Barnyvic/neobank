package com.vaultpay.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull(message = "Wallet ID is required")
        Long walletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "100.00", message = "Minimum withdrawal amount is 100.00")
        BigDecimal amount,

        @NotBlank(message = "Bank code is required")
        String bankCode,

        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotBlank(message = "Transaction PIN is required")
        String transactionPin,

        String idempotencyKey
) {}
