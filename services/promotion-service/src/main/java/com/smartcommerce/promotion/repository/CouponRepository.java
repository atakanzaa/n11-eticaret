package com.smartcommerce.promotion.repository;

import com.smartcommerce.promotion.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCode(String code);
    boolean existsByCode(String code);

    @Query(value = "SELECT * FROM coupons WHERE applicable_seller_ids @> CAST(:sellerIdJson AS jsonb) " +
        "ORDER BY created_at DESC", nativeQuery = true)
    List<Coupon> findByApplicableSellerId(@Param("sellerIdJson") String sellerIdJson);

    @Query("""
        select c from Coupon c
        where c.active = true
          and c.validFrom <= :now
          and c.validUntil > :now
        order by c.validUntil asc
        """)
    List<Coupon> findCurrentlyActive(@Param("now") java.time.Instant now);
}
