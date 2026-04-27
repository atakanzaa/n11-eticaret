package com.smartcommerce.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    T data,
    Meta meta
) {
    @Builder
    public record Meta(
        Instant timestamp,
        String correlationId
    ) {}

    public static <T> ApiResponse<T> of(T data, String correlationId) {
        return ApiResponse.<T>builder()
            .data(data)
            .meta(Meta.builder()
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build())
            .build();
    }
}
