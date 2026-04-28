package com.smartcommerce.order.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderInternalResponse(
    UUID id,
    UUID userId,
    String status,
    BigDecimal grandTotal,
    String currency,
    UUID paymentId,
    String shippingFullName,
    String shippingPhone,
    String shippingCity,
    String shippingDistrict,
    String shippingFullAddress,
    String shippingPostalCode,
    List<OrderInternalItem> items
) {
    public record OrderInternalItem(
        UUID offerId,
        UUID productId,
        UUID sellerId,
        int quantity,
        BigDecimal unitPrice,
        String productTitle
    ) {}
}
