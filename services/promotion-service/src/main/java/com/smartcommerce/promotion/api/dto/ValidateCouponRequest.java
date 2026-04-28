package com.smartcommerce.promotion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ValidateCouponRequest(
    @NotBlank String code,
    @NotNull UUID userId,
    @NotNull @PositiveOrZero BigDecimal cartTotal,
    @PositiveOrZero BigDecimal shippingCost,
    List<UUID> productIds,
    List<UUID> categoryIds,
    List<UUID> sellerIds,
    boolean firstOrder
) {
}
