package com.smartcommerce.promotion.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateCampaignRequest(
    @NotNull UUID offerId,
    @NotNull UUID productId,
    @NotNull @DecimalMin("0.01") BigDecimal discountedPrice,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt
) {
}
