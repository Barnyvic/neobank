package com.vaultpay.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateWalletRequest {

    @NotNull(message = "Currency is required")
    private String currency;
}
