package com.smartcommerce.shipment.api.mapper;

import com.smartcommerce.shipment.api.dto.ShipmentEventResponse;
import com.smartcommerce.shipment.api.dto.ShipmentResponse;
import com.smartcommerce.shipment.domain.Shipment;
import com.smartcommerce.shipment.domain.ShipmentEventLog;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {
    public ShipmentResponse toResponse(Shipment s) {
        return new ShipmentResponse(
            s.getId(), s.getOrderId(), s.getSellerId(), s.getCargoProvider(),
            s.getTrackingNumber(), s.getTrackingUrl(),
            s.getRecipientFullName(), s.getCity(), s.getDistrict(),
            s.getStatus(), s.getEstimatedDeliveryDate(),
            s.getDispatchedAt(), s.getDeliveredAt(), s.getCreatedAt());
    }

    public ShipmentEventResponse toEventResponse(ShipmentEventLog e) {
        return new ShipmentEventResponse(e.getId(), e.getEventType(), e.getDescription(),
            e.getLocation(), e.getOccurredAt());
    }
}
