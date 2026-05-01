package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {
    int countByReviewId(UUID reviewId);
    List<ReviewImage> findByReviewIdOrderByDisplayOrder(UUID reviewId);
}
