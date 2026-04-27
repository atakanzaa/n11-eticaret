package com.smartcommerce.inventory.api.dto;

import com.smartcommerce.inventory.domain.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
    UUID id,
    String reservationKey,
    UUID orderId,
    UUID offerId,
    int quantity,
    ReservationStatus status,
    Instant expiresAt
) {
}
