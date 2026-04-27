package com.smartcommerce.common.errors;

import org.springframework.http.HttpStatus;

public class ValidationException extends BusinessException {

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }
}
