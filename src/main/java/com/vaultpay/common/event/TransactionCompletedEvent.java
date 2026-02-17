package com.vaultpay.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TransactionCompletedEvent extends ApplicationEvent {

    private final Long transactionId;
    private final String reference;

    public TransactionCompletedEvent(Object source, Long transactionId, String reference) {
        super(source);
        this.transactionId = transactionId;
        this.reference = reference;
    }
}
