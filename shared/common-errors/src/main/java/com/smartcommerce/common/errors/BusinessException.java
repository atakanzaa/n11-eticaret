package com.smartcommerce.common.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    public BusinessException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
