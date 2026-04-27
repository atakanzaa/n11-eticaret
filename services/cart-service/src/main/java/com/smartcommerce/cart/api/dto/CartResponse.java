package com.smartcommerce.cart.api.dto;

import com.smartcommerce.cart.domain.CartStatus;

import java.math.BigDecimal;
import java.util.*;

public record CartResponse(
    UUID id,
    UUID userId,
    CartStatus status,
    List<CartItemDto> items,
    BigDecimal total,
    int itemCount
) {
}
