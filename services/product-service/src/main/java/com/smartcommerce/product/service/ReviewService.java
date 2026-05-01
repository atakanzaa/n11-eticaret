package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.common.events.payload.ReviewEventPayload;
import com.smartcommerce.product.api.dto.*;
import com.smartcommerce.product.api.mapper.ReviewMapper;
import com.smartcommerce.product.domain.*;
import com.smartcommerce.product.repository.*;
import com.smartcommerce.product.event.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReviewMapper reviewMapper;
    private final OutboxService outboxService;

    // ── List (public) ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ReviewResponse> listByProduct(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatusAndDeletedAtIsNull(
                productId, ReviewStatus.APPROVED, pageable)
            .map(reviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listFiltered(UUID productId, Integer rating,
                                              Boolean verifiedOnly, Instant fromDate,
                                              Instant toDate, Pageable pageable) {
        return reviewRepository.findFiltered(productId, rating, verifiedOnly, fromDate, toDate, pageable)
            .map(reviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getById(UUID id) {
        return reviewRepository.findByIdAndStatusAndDeletedAtIsNull(id, ReviewStatus.APPROVED)
            .map(reviewMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<ReviewResponse> findMineForProduct(UUID productId, UUID userId) {
        return reviewRepository.findByProductIdAndUserIdAndDeletedAtIsNull(productId, userId)
            .map(reviewMapper::toResponse);
    }

    // ── Create ────────────────────────────────────────────────────────
    @Transactional
    public ReviewResponse create(UUID productId, UUID userId, String userDisplayName, CreateReviewRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found: " + productId);
        }
        if (reviewRepository.existsByProductIdAndUserIdAndDeletedAtIsNull(productId, userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Bu urun icin zaten bir degerlendirmeniz var.");
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
            .verifiedPurchase(request.orderId() != null)
            .helpfulCount(0)
            .unhelpfulCount(0)
            .status(ReviewStatus.PENDING)
            .build();

        var saved = reviewRepository.save(review);

        // Add images
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            IntStream.range(0, request.imageUrls().size()).forEach(i -> {
                var img = ReviewImage.builder()
                    .review(saved)
                    .imageUrl(request.imageUrls().get(i))
                    .displayOrder(i)
                    .build();
                saved.getImages().add(img);
            });
            reviewRepository.flush();
        }

        outboxService.publish(Topics.REVIEW_CREATED, EventType.REVIEW_CREATED,
            saved.getId().toString(), "REVIEW", buildPayload(saved));

        return reviewMapper.toResponse(saved);
    }

    // ── Update (owner only) ───────────────────────────────────────────
    @Transactional
    public ReviewResponse update(UUID reviewId, UUID userId, UpdateReviewRequest request) {
        var review = findOwnReview(reviewId, userId);
        boolean ratingChanged = false;

        if (request.rating() != null) {
            ratingChanged = review.getRating() != request.rating();
            review.setRating(request.rating());
        }
        if (request.title() != null) review.setTitle(request.title());
        if (request.comment() != null) review.setComment(request.comment());

        var updated = reviewRepository.save(review);
        if (ratingChanged && review.getStatus() == ReviewStatus.APPROVED) {
            reAggregate(review.getProductId());
        }

        outboxService.publish(Topics.REVIEW_UPDATED, EventType.REVIEW_UPDATED,
            updated.getId().toString(), "REVIEW", buildPayload(updated));

        return reviewMapper.toResponse(updated);
    }

    // ── Delete (soft, owner or admin) ─────────────────────────────────
    @Transactional
    public void delete(UUID reviewId, UUID userId, boolean isAdmin) {
        var review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + reviewId));

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Bu yorumu silme yetkiniz yok.");
        }

        review.setDeletedAt(Instant.now());
        reviewRepository.save(review);

        if (review.getStatus() == ReviewStatus.APPROVED) {
            reAggregate(review.getProductId());
        }

        outboxService.publish(Topics.REVIEW_DELETED, EventType.REVIEW_DELETED,
            review.getId().toString(), "REVIEW", buildPayload(review));
    }

    // ── Moderation (admin) ────────────────────────────────────────────
    @Transactional
    public ReviewResponse approve(UUID reviewId, UUID moderatorId) {
        var review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + reviewId));

        review.setStatus(ReviewStatus.APPROVED);
        review.setModeratedAt(Instant.now());
        review.setModeratedBy(moderatorId);
        reviewRepository.save(review);
        reAggregate(review.getProductId());

        outboxService.publish(Topics.REVIEW_APPROVED, EventType.REVIEW_APPROVED,
            review.getId().toString(), "REVIEW", buildPayload(review));

        return reviewMapper.toResponse(review);
    }

    @Transactional
    public ReviewResponse reject(UUID reviewId, UUID moderatorId, String rejectionReason) {
        var review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + reviewId));

        review.setStatus(ReviewStatus.REJECTED);
        review.setModeratedAt(Instant.now());
        review.setModeratedBy(moderatorId);
        review.setRejectionReason(rejectionReason);
        reviewRepository.save(review);

        outboxService.publish(Topics.REVIEW_REJECTED, EventType.REVIEW_REJECTED,
            review.getId().toString(), "REVIEW", buildPayload(review));

        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listPending(Pageable pageable) {
        return reviewRepository.findByStatusAndDeletedAtIsNull(ReviewStatus.PENDING, pageable)
            .map(reviewMapper::toResponse);
    }

    // ── Voting ────────────────────────────────────────────────────────
    @Transactional
    public ReviewResponse vote(UUID reviewId, UUID userId, VoteType voteType) {
        var review = reviewRepository.findByIdAndStatusAndDeletedAtIsNull(reviewId, ReviewStatus.APPROVED)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + reviewId));

        var existing = reviewVoteRepository.findByReviewIdAndUserId(reviewId, userId);
        if (existing.isPresent()) {
            var vote = existing.get();
            if (vote.getVoteType() == voteType) {
                // Same vote again — remove it (toggle off)
                reviewVoteRepository.delete(vote);
            } else {
                // Change vote type
                vote.setVoteType(voteType);
                reviewVoteRepository.save(vote);
            }
        } else {
            reviewVoteRepository.save(ReviewVote.builder()
                .reviewId(reviewId).userId(userId).voteType(voteType).build());
        }

        // Recompute counts
        review.setHelpfulCount((int) reviewVoteRepository.countByReviewIdAndVoteType(reviewId, VoteType.HELPFUL));
        review.setUnhelpfulCount((int) reviewVoteRepository.countByReviewIdAndVoteType(reviewId, VoteType.UNHELPFUL));
        reviewRepository.save(review);

        return reviewMapper.toResponse(review);
    }

    // ── Stats ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ReviewStatsResponse getStats(UUID productId) {
        var rows = reviewRepository.ratingDistribution(productId);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0L);
        long total = 0;
        double sum = 0;
        for (Object[] row : rows) {
            int rating = (int) row[0];
            long count = (long) row[1];
            distribution.put(rating, count);
            total += count;
            sum += rating * count;
        }
        double avg = total > 0 ? Math.round((sum / total) * 10.0) / 10.0 : 0.0;
        return new ReviewStatsResponse(avg, total, distribution);
    }

    // ── Reporting ─────────────────────────────────────────────────────
    @Transactional
    public ReviewReportResponse report(UUID reviewId, UUID userId, CreateReviewReportRequest request) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + reviewId);
        }
        if (reviewReportRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Bu yorumu zaten raporladiniz.");
        }
        var report = ReviewReport.builder()
            .reviewId(reviewId).userId(userId)
            .reason(request.reason()).description(request.description())
            .status(ReportStatus.PENDING).build();
        return reviewMapper.toReportResponse(reviewReportRepository.save(report));
    }

    // ── Rating aggregation (reusable) ─────────────────────────────────
    @Transactional(readOnly = true)
    public RatingSummary aggregate(UUID productId) {
        Object[] row = reviewRepository.aggregateRating(productId);
        if (row.length > 0 && row[0] instanceof Object[] inner) {
            row = inner;
        }
        double avg = row[0] == null ? 0.0 : ((Number) row[0]).doubleValue();
        long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
        return new RatingSummary(Math.round(avg * 10.0) / 10.0, count);
    }

    public record RatingSummary(double averageRating, long reviewCount) {}

    // ── Private helpers ───────────────────────────────────────────────
    private Review findOwnReview(UUID reviewId, UUID userId) {
        var review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + reviewId));
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Bu yorumu duzenleme yetkiniz yok.");
        }
        return review;
    }

    private void reAggregate(UUID productId) {
        var summary = aggregate(productId);
        var product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setAverageRating(summary.averageRating());
            product.setReviewCount((int) summary.reviewCount());
            productRepository.save(product);
        }
    }

    private ReviewEventPayload buildPayload(Review review) {
        return ReviewEventPayload.builder()
            .reviewId(review.getId())
            .productId(review.getProductId())
            .userId(review.getUserId())
            .rating(review.getRating())
            .title(review.getTitle())
            .status(review.getStatus().name())
            .rejectionReason(review.getRejectionReason())
            .verifiedPurchase(review.isVerifiedPurchase())
            .occurredAt(Instant.now())
            .build();
    }
}
