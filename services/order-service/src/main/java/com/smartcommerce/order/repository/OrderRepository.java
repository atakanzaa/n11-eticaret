package com.smartcommerce.order.repository;

import com.smartcommerce.order.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    List<Order> findByStatusAndExpiresAtLessThanEqual(OrderStatus status, Instant now);
    long countByUserId(UUID userId);

    /** Sum of line_total for the seller's CONFIRMED orders since :since. */
    @Query(value = """
        SELECT COALESCE(SUM(oi.line_total), 0)
        FROM orders o JOIN order_items oi ON oi.order_id = o.id
        WHERE oi.seller_id = :sellerId
          AND o.status = 'CONFIRMED'
          AND o.created_at >= :since
        """, nativeQuery = true)
    BigDecimal sumSellerRevenueSince(@Param("sellerId") UUID sellerId, @Param("since") Instant since);

    /** Pending orders (PENDING or CONFIRMED but not yet shipped) for the seller. */
    @Query(value = """
        SELECT COUNT(DISTINCT o.id)
        FROM orders o JOIN order_items oi ON oi.order_id = o.id
        WHERE oi.seller_id = :sellerId
          AND o.status IN ('PENDING', 'CONFIRMED')
        """, nativeQuery = true)
    long countPendingForSeller(@Param("sellerId") UUID sellerId);

    /**
     * Daily revenue rollup per seller: rows of (day, totalRevenue, orderCount)
     * for the last :days days. Used for seller dashboard revenue chart.
     */
    @Query(value = """
        SELECT DATE_TRUNC('day', o.created_at) AS day,
               COALESCE(SUM(oi.line_total), 0) AS revenue,
               COUNT(DISTINCT o.id) AS orderCount
        FROM orders o JOIN order_items oi ON oi.order_id = o.id
        WHERE oi.seller_id = :sellerId
          AND o.status = 'CONFIRMED'
          AND o.created_at >= :since
        GROUP BY DATE_TRUNC('day', o.created_at)
        ORDER BY day ASC
        """, nativeQuery = true)
    List<Object[]> sellerRevenueByDay(@Param("sellerId") UUID sellerId, @Param("since") Instant since);

    /** Platform GMV (sum of grand_total of CONFIRMED orders) since :since. */
    @Query(value = """
        SELECT COALESCE(SUM(o.grand_total), 0)
        FROM orders o
        WHERE o.status = 'CONFIRMED' AND o.created_at >= :since
        """, nativeQuery = true)
    BigDecimal sumGmvSince(@Param("since") Instant since);

    @Query(value = """
        SELECT COUNT(*)
        FROM orders o
        WHERE o.created_at >= :since AND o.status NOT IN ('EXPIRED')
        """, nativeQuery = true)
    long countOrdersSince(@Param("since") Instant since);

    @Query(value = """
        SELECT COALESCE(AVG(o.grand_total), 0)
        FROM orders o
        WHERE o.status = 'CONFIRMED' AND o.created_at >= :since
        """, nativeQuery = true)
    BigDecimal averageBasketSince(@Param("since") Instant since);

    /** Distinct seller_ids that have at least one CONFIRMED order since :since. */
    @Query(value = """
        SELECT COUNT(DISTINCT oi.seller_id)
        FROM orders o JOIN order_items oi ON oi.order_id = o.id
        WHERE o.status = 'CONFIRMED' AND o.created_at >= :since
        """, nativeQuery = true)
    long countActiveSellersSince(@Param("since") Instant since);

    /** Distinct orders that contain at least one item from the seller. */
    @Query(value = """
        SELECT DISTINCT o.* FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        WHERE oi.seller_id = :sellerId
        ORDER BY o.created_at DESC
        """,
        countQuery = "SELECT COUNT(DISTINCT o.id) FROM orders o JOIN order_items oi ON oi.order_id = o.id WHERE oi.seller_id = :sellerId",
        nativeQuery = true)
    Page<Order> findDistinctBySellerId(@Param("sellerId") UUID sellerId, Pageable pageable);
}
