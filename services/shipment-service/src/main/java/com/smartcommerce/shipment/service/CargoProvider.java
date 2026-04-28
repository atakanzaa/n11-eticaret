package com.smartcommerce.shipment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CargoProvider {

    CargoCreateResult create(CargoCreateRequest request);

    String getName();

    record CargoCreateRequest(
        UUID shipmentId,
        String recipientName,
        String recipientPhone,
        String fullAddress,
        String city,
        String district,
        String postalCode,
        BigDecimal weight,
        BigDecimal declaredValue
    ) {}

    record CargoCreateResult(
        boolean success,
        String trackingNumber,
        String trackingUrl,
        LocalDate estimatedDeliveryDate,
        String errorMessage
    ) {}
}
