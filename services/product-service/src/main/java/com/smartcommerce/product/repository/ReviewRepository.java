package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.Review;
import com.smartcommerce.product.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByIdAndDeletedAtIsNull(UUID id);

    Page<Review> findByProductIdAndStatusAndDeletedAtIsNull(UUID productId, ReviewStatus status, Pageable pageable);

    boolean existsByProductIdAndUserIdAndDeletedAtIsNull(UUID productId, UUID userId);

    Optional<Review> findByProductIdAndUserIdAndDeletedAtIsNull(UUID productId, UUID userId);

    Page<Review> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Review> findByIdAndStatusAndDeletedAtIsNull(UUID id, ReviewStatus status);

    // Moderation queue
    Page<Review> findByStatusAndDeletedAtIsNull(ReviewStatus status, Pageable pageable);

    // Rating aggregate (only APPROVED, non-deleted)
    @Query("""
        SELECT COALESCE(AVG(CAST(r.rating AS double)), 0.0), COUNT(r)
        FROM Review r
        WHERE r.productId = :productId
          AND r.status = com.smartcommerce.product.domain.ReviewStatus.APPROVED
          AND r.deletedAt IS NULL
        """)
    Object[] aggregateRating(@Param("productId") UUID productId);

    // Rating distribution
    @Query("""
        SELECT r.rating, COUNT(r) FROM Review r
        WHERE r.productId = :productId
          AND r.status = com.smartcommerce.product.domain.ReviewStatus.APPROVED
          AND r.deletedAt IS NULL
        GROUP BY r.rating
        ORDER BY r.rating
        """)
    List<Object[]> ratingDistribution(@Param("productId") UUID productId);

    // Advanced filtering
    @Query("""
        SELECT r FROM Review r
        WHERE r.productId = :productId
          AND r.status = com.smartcommerce.product.domain.ReviewStatus.APPROVED
          AND r.deletedAt IS NULL
          AND (:rating IS NULL OR r.rating = :rating)
          AND (:verifiedOnly IS NULL OR r.verifiedPurchase = :verifiedOnly)
          AND (:fromDate IS NULL OR r.createdAt >= :fromDate)
          AND (:toDate IS NULL OR r.createdAt <= :toDate)
        """)
    Page<Review> findFiltered(@Param("productId") UUID productId,
                              @Param("rating") Integer rating,
                              @Param("verifiedOnly") Boolean verifiedOnly,
                              @Param("fromDate") Instant fromDate,
                              @Param("toDate") Instant toDate,
                              Pageable pageable);
}
