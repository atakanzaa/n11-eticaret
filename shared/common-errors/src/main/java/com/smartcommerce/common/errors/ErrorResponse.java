package com.smartcommerce.common.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    Error error
) {
    @Builder
    public record Error(
        String code,
        String message,
        List<FieldError> details,
        String correlationId,
        Instant timestamp
    ) {}

    @Builder
    public record FieldError(
        String field,
        String message
    ) {}
}
