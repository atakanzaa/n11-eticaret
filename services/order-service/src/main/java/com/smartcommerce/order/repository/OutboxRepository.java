package com.smartcommerce.order.repository;

import com.smartcommerce.order.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
