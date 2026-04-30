package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.Review;
import com.smartcommerce.product.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    /** Returns [averageRating, count] for the product's APPROVED reviews; both 0 when none. */
    @Query("""
        SELECT COALESCE(AVG(CAST(r.rating AS double)), 0.0), COUNT(r)
        FROM Review r
        WHERE r.productId = :productId AND r.status = com.smartcommerce.product.domain.ReviewStatus.APPROVED
        """)
    Object[] aggregateRating(UUID productId);

    Optional<Review> findByIdAndStatus(UUID id, ReviewStatus status);
}
