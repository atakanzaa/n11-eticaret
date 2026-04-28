package com.smartcommerce.promotion.api.dto;

import com.smartcommerce.promotion.domain.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    @NotNull DiscountType discountType,
    @NotNull @PositiveOrZero BigDecimal discountValue,
    BigDecimal maxDiscountAmount,
    BigDecimal minimumOrderAmount,
    Integer totalUsageLimit,
    Integer perUserLimit,
    @NotNull Instant validFrom,
    @NotNull Instant validUntil,
    Boolean firstOrderOnly,
    Boolean stackable
) {
}
