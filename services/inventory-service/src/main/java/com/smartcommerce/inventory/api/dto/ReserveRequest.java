package com.smartcommerce.inventory.api.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record ReserveRequest(
    @NotNull UUID orderId,
    @NotNull UUID offerId,
    @Min(1) int quantity,
    @NotBlank String reservationKey
) {
}
