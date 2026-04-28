package com.smartcommerce.shipment.repository;

import com.smartcommerce.shipment.domain.OutboxEvent;
import com.smartcommerce.shipment.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
