package com.vaultpay.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InitializePaymentResponse {

    private boolean status;
    private String message;
    private PaymentData data;

    @Data
    public static class PaymentData {

        @JsonProperty("authorization_url")
        private String authorizationUrl;

        @JsonProperty("access_code")
        private String accessCode;

        private String reference;
    }
}
