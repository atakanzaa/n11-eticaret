package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, UUID> {
    Optional<ReviewReply> findByReviewId(UUID reviewId);
    boolean existsByReviewId(UUID reviewId);
    List<ReviewReply> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);
}
