package com.smartcommerce.inventory.repository;

import com.smartcommerce.inventory.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
