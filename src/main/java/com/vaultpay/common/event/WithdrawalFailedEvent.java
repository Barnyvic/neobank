package com.vaultpay.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class WithdrawalFailedEvent extends ApplicationEvent {

    private final Long transactionId;
    private final String reference;
    private final String bankCode;
    private final String accountNumber;
    private final BigDecimal amount;
    private final String currency;
    private final int attemptNumber;

    public WithdrawalFailedEvent(Object source, Long transactionId, String reference,
                                  String bankCode, String accountNumber,
                                  BigDecimal amount, String currency, int attemptNumber) {
        super(source);
        this.transactionId = transactionId;
        this.reference = reference;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.currency = currency;
        this.attemptNumber = attemptNumber;
    }
}
