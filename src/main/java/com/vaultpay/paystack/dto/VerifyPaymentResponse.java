package com.vaultpay.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VerifyPaymentResponse {

    private boolean status;
    private String message;
    private VerificationData data;

    @Data
    public static class VerificationData {

        private String status;
        private String reference;
        private BigDecimal amount;
        private String currency;

        @JsonProperty("paid_at")
        private String paidAt;

        private String channel;
    }
}
