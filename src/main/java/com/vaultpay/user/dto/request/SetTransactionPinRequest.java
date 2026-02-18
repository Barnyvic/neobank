package com.vaultpay.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetTransactionPinRequest(
        @NotBlank(message = "Transaction PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
        String pin,
        @NotBlank(message = "PIN confirmation is required")
        String confirmPin
) {}
