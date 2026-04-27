package com.smartcommerce.cart.api.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record AddItemRequest(
    @NotNull UUID offerId,
    @Min(1) @Max(99) int quantity
) {
}
