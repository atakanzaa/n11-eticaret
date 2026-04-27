package com.smartcommerce.order.repository;

import com.smartcommerce.order.domain.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface SagaLogRepository extends JpaRepository<SagaLog, UUID> {
    List<SagaLog> findByOrderIdOrderByStartedAtAsc(UUID orderId);
}
