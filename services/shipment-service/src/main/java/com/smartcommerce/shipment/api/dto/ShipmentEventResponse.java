package com.smartcommerce.shipment.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ShipmentEventResponse(
    UUID id,
    String eventType,
    String description,
    String location,
    Instant occurredAt
) {
}
