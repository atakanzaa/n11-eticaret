package com.smartcommerce.returns.repository;

import com.smartcommerce.returns.domain.ReturnEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReturnRepository extends JpaRepository<ReturnEntity, UUID> {
    List<ReturnEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ReturnEntity> findByOrderId(UUID orderId);
}
