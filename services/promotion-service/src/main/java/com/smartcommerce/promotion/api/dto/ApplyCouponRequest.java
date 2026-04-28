package com.smartcommerce.promotion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyCouponRequest(
    @NotBlank String code,
    @NotNull UUID userId,
    @NotNull UUID orderId,
    @NotNull @Positive BigDecimal discountAmount
) {
}
