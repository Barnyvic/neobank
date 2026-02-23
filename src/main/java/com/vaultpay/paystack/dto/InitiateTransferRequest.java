package com.vaultpay.paystack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateTransferRequest {

    private String source;
    private String reason;
    private String amount;
    private String recipient;
    private String reference;
    private String currency;
}
