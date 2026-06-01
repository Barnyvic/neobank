package com.vaultpay.fraud.dto.request;

import com.vaultpay.fraud.enums.FraudAlertStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveFraudAlertRequest(
        @NotNull FraudAlertStatus status,
        @Size(max = 500) String note) {
}
