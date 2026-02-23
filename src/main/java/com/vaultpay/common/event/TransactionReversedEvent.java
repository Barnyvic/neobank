package com.vaultpay.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TransactionReversedEvent extends ApplicationEvent {

    private final String originalReference;
    private final String reversalReference;
    private final Long walletId;

    public TransactionReversedEvent(Object source, String originalReference,
                                     String reversalReference, Long walletId) {
        super(source);
        this.originalReference = originalReference;
        this.reversalReference = reversalReference;
        this.walletId = walletId;
    }
}
