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
public class TransferRecipientRequest {

    private String type;
    private String name;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("bank_code")
    private String bankCode;

    private String currency;
}
