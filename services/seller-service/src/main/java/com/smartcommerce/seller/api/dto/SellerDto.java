package com.smartcommerce.seller.api.dto;

import com.smartcommerce.seller.domain.SellerStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SellerDto(
    UUID id,
    UUID userId,
    String storeName,
    String description,
    BigDecimal rating,
    Integer ratingCount,
    SellerStatus status,
    BigDecimal commissionRate,
    Instant createdAt
) {}
