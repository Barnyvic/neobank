package com.vaultpay.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundWalletRequest {

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum funding amount is 100.00")
    private BigDecimal amount;

    private String idempotencyKey;
}
