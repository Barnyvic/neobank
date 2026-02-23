package com.vaultpay.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Recipient wallet number is required")
        String recipientWalletNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00")
        BigDecimal amount,

        String description,

        @NotBlank(message = "Transaction PIN is required")
        String transactionPin,

        String idempotencyKey
) {}
