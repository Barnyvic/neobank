package com.vaultpay.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetTransactionPinRequest(
        @NotBlank(message = "Transaction PIN is required")
        @Pattern(regexp = "^\\d{4,6}$", message = "PIN must be 4-6 digits")
        String pin,
        @NotBlank(message = "PIN confirmation is required")
        @Pattern(regexp = "^\\d{4,6}$", message = "PIN must be 4-6 digits")
        String confirmPin
) {}
