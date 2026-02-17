package com.vaultpay.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WebhookPayload {

    private String event;
    private WebhookData data;

    @Data
    public static class WebhookData {

        private String status;
        private String reference;
        private BigDecimal amount;
        private String currency;

        @JsonProperty("paid_at")
        private String paidAt;

        private String channel;
    }
}
