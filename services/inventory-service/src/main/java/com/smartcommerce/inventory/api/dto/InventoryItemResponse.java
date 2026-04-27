package com.smartcommerce.inventory.api.dto;

import java.util.UUID;

public record InventoryItemResponse(
    UUID id,
    UUID offerId,
    UUID productId,
    UUID sellerId,
    int availableQuantity,
    int reservedQuantity,
    int soldQuantity,
    int incomingQuantity,
    int lowStockThreshold,
    boolean allowBackorder,
    Long version
) {
}
