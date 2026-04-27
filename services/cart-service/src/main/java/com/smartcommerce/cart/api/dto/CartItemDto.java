package com.smartcommerce.cart.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDto(
    UUID id,
    UUID offerId,
    UUID productId,
    UUID sellerId,
    int quantity,
    BigDecimal unitPriceSnapshot,
    String currencySnapshot,
    String productTitleSnapshot,
    String productImageSnapshot,
    String sellerNameSnapshot,
    BigDecimal cargoPriceSnapshot,
    BigDecimal subtotal
) {
}
