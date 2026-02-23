package com.vaultpay.wallet.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateWalletRequest(
        @NotNull(message = "Currency is required")
        String currency
) {}
