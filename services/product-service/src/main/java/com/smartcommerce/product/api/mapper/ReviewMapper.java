package com.smartcommerce.product.api.mapper;

import com.smartcommerce.product.api.dto.ReviewResponse;
import com.smartcommerce.product.domain.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getProductId(),
            review.getUserDisplayName(),
            review.getRating(),
            review.getTitle(),
            review.getComment(),
            review.getVariantInfo(),
            review.getHelpfulCount(),
            review.getCreatedAt()
        );
    }
}
