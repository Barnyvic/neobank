package com.vaultpay.transaction.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull(message = "Wallet ID is required")
        @Positive(message = "Wallet ID must be positive")
        Long walletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "100.00", message = "Minimum withdrawal amount is 100.00")
        BigDecimal amount,

        @NotBlank(message = "Bank code is required")
        @Pattern(regexp = "^\\d{3,6}$", message = "Bank code must be 3-6 digits")
        String bankCode,

        @NotBlank(message = "Account number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Account number must be exactly 10 digits")
        String accountNumber,

        @NotBlank(message = "Transaction PIN is required")
        @Pattern(regexp = "^\\d{4,6}$", message = "PIN must be 4-6 digits")
        String transactionPin,

        String idempotencyKey
) {}
