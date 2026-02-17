package com.vaultpay.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {

    private Long walletId;
    private String walletNumber;
    private BigDecimal availableBalance;
    private String currency;
}
