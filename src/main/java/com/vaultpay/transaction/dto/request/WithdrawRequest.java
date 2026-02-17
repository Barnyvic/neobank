package com.vaultpay.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum withdrawal amount is 100.00")
    private BigDecimal amount;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Transaction PIN is required")
    private String transactionPin;

    private String idempotencyKey;
}
