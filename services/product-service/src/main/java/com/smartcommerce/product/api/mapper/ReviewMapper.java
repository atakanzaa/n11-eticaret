package com.smartcommerce.product.api.mapper;

import com.smartcommerce.product.api.dto.ReviewReplyResponse;
import com.smartcommerce.product.api.dto.ReviewReportResponse;
import com.smartcommerce.product.api.dto.ReviewResponse;
import com.smartcommerce.product.domain.Review;
import com.smartcommerce.product.domain.ReviewImage;
import com.smartcommerce.product.domain.ReviewReply;
import com.smartcommerce.product.domain.ReviewReport;
import org.springframework.stereotype.Component;

import java.util.List;

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
            review.getUnhelpfulCount(),
            review.isVerifiedPurchase(),
            review.getImages() != null
                ? review.getImages().stream().map(ReviewImage::getImageUrl).toList()
                : List.of(),
            review.getReply() != null ? toReplyResponse(review.getReply()) : null,
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }

    public ReviewReplyResponse toReplyResponse(ReviewReply reply) {
        return new ReviewReplyResponse(
            reply.getId(),
            reply.getSellerId(),
            reply.getContent(),
            reply.getCreatedAt(),
            reply.getUpdatedAt()
        );
    }

    public ReviewReportResponse toReportResponse(ReviewReport report) {
        return new ReviewReportResponse(
            report.getId(),
            report.getReviewId(),
            report.getUserId(),
            report.getReason(),
            report.getDescription(),
            report.getStatus().name(),
            report.getCreatedAt()
        );
    }
}
