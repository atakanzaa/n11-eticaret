package com.smartcommerce.inventory.api.dto;

public record SellerInventoryStatsResponse(
    long totalOffers,
    long lowStockCount
) {
}
