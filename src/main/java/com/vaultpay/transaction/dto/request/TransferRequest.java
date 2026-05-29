package com.vaultpay.transaction.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Recipient wallet number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Wallet number must be exactly 10 digits")
        String recipientWalletNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00")
        BigDecimal amount,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotBlank(message = "Transaction PIN is required")
        @Pattern(regexp = "^\\d{4,6}$", message = "PIN must be 4-6 digits")
        String transactionPin,

        String idempotencyKey
) {}
