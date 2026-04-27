package com.smartcommerce.common.errors;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }
}
