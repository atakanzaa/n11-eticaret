package com.smartcommerce.fraud.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record FraudCheckRequest(
    @NotNull UUID orderId,
    @NotNull UUID userId,
    @NotNull @PositiveOrZero BigDecimal amount,
    String currency,
    int itemCount,
    String userIp,
    boolean firstOrder
) {
}
