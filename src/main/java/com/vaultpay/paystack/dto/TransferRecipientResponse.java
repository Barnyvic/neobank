package com.vaultpay.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransferRecipientResponse {

    private boolean status;
    private String message;
    private RecipientData data;

    @Data
    public static class RecipientData {

        @JsonProperty("recipient_code")
        private String recipientCode;

        private String name;
        private String type;
    }
}
