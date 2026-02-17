package com.vaultpay.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INSUFFICIENT_FUNDS);
    }

    public InsufficientFundsException() {
        this("Insufficient funds to complete this transaction");
    }
}
