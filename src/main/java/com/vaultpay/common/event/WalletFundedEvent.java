package com.vaultpay.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class WalletFundedEvent extends ApplicationEvent {

    private final Long walletId;
    private final BigDecimal amount;
    private final String reference;

    public WalletFundedEvent(Object source, Long walletId, BigDecimal amount, String reference) {
        super(source);
        this.walletId = walletId;
        this.amount = amount;
        this.reference = reference;
    }
}
