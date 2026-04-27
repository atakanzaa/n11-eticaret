package com.smartcommerce.order.api.dto;

import com.smartcommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public record OrderResponse(
    UUID id,
    String orderNumber,
    UUID userId,
    OrderStatus status,
    BigDecimal grandTotal,
    String currency,
    String sagaState,
    Instant expiresAt,
    List<OrderItemResponse> items
) {
}
