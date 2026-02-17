package com.vaultpay.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException() {
        this("Unauthorized access");
    }
}
