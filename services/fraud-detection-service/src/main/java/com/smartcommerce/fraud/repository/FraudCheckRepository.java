package com.smartcommerce.fraud.repository;

import com.smartcommerce.fraud.domain.FraudCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FraudCheckRepository extends JpaRepository<FraudCheck, UUID> {
    Optional<FraudCheck> findByOrderId(UUID orderId);
    Page<FraudCheck> findByDecision(String decision, Pageable pageable);
}
