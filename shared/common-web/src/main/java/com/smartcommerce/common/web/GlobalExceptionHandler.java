package com.smartcommerce.common.web;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        var error = ErrorResponse.Error.builder()
            .code(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .correlationId(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorResponse.builder().error(error).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> ErrorResponse.FieldError.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build())
            .toList();
        var error = ErrorResponse.Error.builder()
            .code("ERR_1001")
            .message("Validation failed")
            .details(details)
            .correlationId(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.badRequest().body(ErrorResponse.builder().error(error).build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        var details = ex.getConstraintViolations().stream()
            .map(cv -> ErrorResponse.FieldError.builder()
                .field(cv.getPropertyPath().toString())
                .message(cv.getMessage())
                .build())
            .toList();
        var error = ErrorResponse.Error.builder()
            .code("ERR_1001")
            .message("Constraint violation")
            .details(details)
            .correlationId(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.badRequest().body(ErrorResponse.builder().error(error).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        var error = ErrorResponse.Error.builder()
            .code("ERR_1000")
            .message("Internal server error")
            .correlationId(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder().error(error).build());
    }
}
