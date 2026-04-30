package com.smartcommerce.order.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CheckoutResponse(
    UUID orderId,
    String orderNumber,
    String status,
    BigDecimal grandTotal,
    Instant expiresAt
) {
}
