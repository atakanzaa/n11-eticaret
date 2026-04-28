package com.smartcommerce.shipment.api.dto;

import com.smartcommerce.shipment.domain.ShipmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ShipmentResponse(
    UUID id,
    UUID orderId,
    UUID sellerId,
    String cargoProvider,
    String trackingNumber,
    String trackingUrl,
    String recipientFullName,
    String city,
    String district,
    ShipmentStatus status,
    LocalDate estimatedDeliveryDate,
    Instant dispatchedAt,
    Instant deliveredAt,
    Instant createdAt
) {
}
