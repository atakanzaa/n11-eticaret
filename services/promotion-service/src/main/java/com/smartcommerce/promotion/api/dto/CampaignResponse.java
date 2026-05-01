package com.smartcommerce.promotion.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(
    UUID id,
    UUID offerId,
    UUID sellerId,
    UUID productId,
    BigDecimal discountedPrice,
    Instant startsAt,
    Instant endsAt,
    boolean active
) {
}
