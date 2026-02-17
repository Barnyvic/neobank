package com.vaultpay.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private String reference;
    private String transactionType;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String sourceWalletNumber;
    private String destWalletNumber;
    private LocalDateTime createdAt;
}
