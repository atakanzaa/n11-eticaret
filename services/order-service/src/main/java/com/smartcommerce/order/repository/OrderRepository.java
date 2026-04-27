package com.smartcommerce.order.repository;

import com.smartcommerce.order.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    List<Order> findByStatusAndExpiresAtLessThanEqual(OrderStatus status, Instant now);
}
