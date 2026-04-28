package com.smartcommerce.promotion.api.dto;

import com.smartcommerce.promotion.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
    UUID id,
    String code,
    String name,
    String description,
    DiscountType discountType,
    BigDecimal discountValue,
    BigDecimal maxDiscountAmount,
    BigDecimal minimumOrderAmount,
    Integer totalUsageLimit,
    Integer perUserLimit,
    Integer timesUsed,
    Instant validFrom,
    Instant validUntil,
    boolean active,
    boolean firstOrderOnly,
    boolean stackable
) {
}
