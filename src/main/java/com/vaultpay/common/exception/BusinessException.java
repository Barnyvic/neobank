package com.vaultpay.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;

    public BusinessException(String message, HttpStatus status, ErrorCode errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        this(message, status, ErrorCode.BAD_REQUEST);
    }
}
