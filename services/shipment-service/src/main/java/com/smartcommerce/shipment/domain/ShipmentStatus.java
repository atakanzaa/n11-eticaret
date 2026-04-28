package com.smartcommerce.shipment.domain;

public enum ShipmentStatus {
    CREATED,
    READY_FOR_PICKUP,
    DISPATCHED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED_DELIVERY,
    RETURNED_TO_SENDER,
    CANCELLED
}
