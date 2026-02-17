package com.vaultpay.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Recipient wallet number is required")
    private String recipientWalletNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "Transaction PIN is required")
    private String transactionPin;

    private String idempotencyKey;
}
