package com.vaultpay.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InitiateTransferResponse {

    private boolean status;
    private String message;
    private TransferData data;

    @Data
    public static class TransferData {

        @JsonProperty("transfer_code")
        private String transferCode;

        private String reference;
        private String status;
        private Long amount;
    }
}
