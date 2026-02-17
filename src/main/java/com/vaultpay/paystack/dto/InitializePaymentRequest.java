package com.vaultpay.paystack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializePaymentRequest {

    private String email;
    private String amount;
    private String reference;
    private String currency;

    @JsonProperty("callback_url")
    private String callbackUrl;
}
