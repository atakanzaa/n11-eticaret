package com.smartcommerce.promotion.repository;

import com.smartcommerce.promotion.domain.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {
    long countByCouponIdAndUserId(UUID couponId, UUID userId);
    Optional<CouponUsage> findByCouponIdAndOrderId(UUID couponId, UUID orderId);
}
