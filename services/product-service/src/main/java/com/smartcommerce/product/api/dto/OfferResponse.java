package com.smartcommerce.product.api.dto;

import com.smartcommerce.product.domain.CargoProvider;
import com.smartcommerce.product.domain.OfferStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OfferResponse(
    UUID id,
    UUID productId,
    UUID sellerId,
    String sku,
    BigDecimal price,
    String currency,
    BigDecimal listPrice,
    CargoProvider cargoProvider,
    BigDecimal cargoPrice,
    int estimatedDeliveryDays,
    BigDecimal freeShippingThreshold,
    OfferStatus status,
    Long version
) {
}
