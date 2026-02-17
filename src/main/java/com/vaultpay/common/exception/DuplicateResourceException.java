package com.vaultpay.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String resource, Object identifier) {
        super(resource + " already exists with identifier: " + identifier, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
