package com.smartcommerce.order.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
    UUID id,
    UUID offerId,
    UUID productId,
    UUID sellerId,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    String productTitle,
    String productImageUrl,
    String sellerName
) {
}
