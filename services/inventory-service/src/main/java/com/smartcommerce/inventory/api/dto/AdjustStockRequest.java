package com.smartcommerce.inventory.api.dto;

import jakarta.validation.constraints.Min;

public record AdjustStockRequest(
    @Min(0) int newAvailableQuantity,
    @Min(0) Integer lowStockThreshold
) {
}
