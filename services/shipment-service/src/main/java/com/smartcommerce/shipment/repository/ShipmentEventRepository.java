package com.smartcommerce.shipment.repository;

import com.smartcommerce.shipment.domain.ShipmentEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEventLog, UUID> {
    List<ShipmentEventLog> findByShipmentIdOrderByOccurredAtAsc(UUID shipmentId);
}
