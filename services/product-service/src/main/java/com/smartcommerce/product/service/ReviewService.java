package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.product.api.dto.CreateReviewRequest;
import com.smartcommerce.product.api.dto.ReviewResponse;
import com.smartcommerce.product.api.mapper.ReviewMapper;
import com.smartcommerce.product.domain.Review;
import com.smartcommerce.product.domain.ReviewStatus;
import com.smartcommerce.product.repository.ProductRepository;
import com.smartcommerce.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ReviewMapper reviewMapper;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listByProduct(UUID productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
        return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable)
            .map(reviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getById(UUID id) {
        return reviewRepository.findByIdAndStatus(id, ReviewStatus.APPROVED)
            .map(reviewMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
    }

    @Transactional
    public ReviewResponse create(UUID productId, UUID userId, String userDisplayName, CreateReviewRequest request) {
        var product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Bu ürün için zaten bir değerlendirmeniz var.");
        }
        var review = Review.builder()
            .productId(productId)
            .userId(userId)
            .userDisplayName(userDisplayName)
            .rating(request.rating())
            .title(request.title())
            .comment(request.comment())
            .variantInfo(request.variantInfo())
            .orderId(request.orderId())
            .helpfulCount(0)
            .status(ReviewStatus.APPROVED) // demo: no moderation queue
            .build();
        var saved = reviewRepository.save(review);
        // Recompute the cached rating on the parent product. SaveAndFlush so the new review is
        // visible to the aggregate query that runs in the same transaction.
        reviewRepository.flush();
        var summary = aggregate(productId);
        product.setAverageRating(summary.averageRating());
        product.setReviewCount((int) summary.reviewCount());
        productRepository.save(product);
        return reviewMapper.toResponse(saved);
    }

    @Transactional
    public ReviewResponse markHelpful(UUID reviewId) {
        var review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return reviewMapper.toResponse(review);
    }

    /** Returns rounded-to-1-decimal average rating and total count for a product. */
    @Transactional(readOnly = true)
    public RatingSummary aggregate(UUID productId) {
        Object[] row = reviewRepository.aggregateRating(productId);
        // JPA aggregate query returns nested Object[] when projecting two scalars.
        if (row.length > 0 && row[0] instanceof Object[] inner) {
            row = inner;
        }
        double avg = row[0] == null ? 0.0 : ((Number) row[0]).doubleValue();
        long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
        return new RatingSummary(Math.round(avg * 10.0) / 10.0, count);
    }

    public record RatingSummary(double averageRating, long reviewCount) {
    }
}
