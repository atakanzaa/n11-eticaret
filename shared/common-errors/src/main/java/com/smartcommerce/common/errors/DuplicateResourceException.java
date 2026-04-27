package com.smartcommerce.common.errors;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
    }
}
