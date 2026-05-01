package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.product.api.dto.CreateReviewReplyRequest;
import com.smartcommerce.product.api.dto.ReviewReplyResponse;
import com.smartcommerce.product.api.dto.UpdateReviewReplyRequest;
import com.smartcommerce.product.api.mapper.ReviewMapper;
import com.smartcommerce.product.domain.ReviewReply;
import com.smartcommerce.product.domain.ReviewStatus;
import com.smartcommerce.product.repository.ReviewReplyRepository;
import com.smartcommerce.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReplyService {
    private final ReviewReplyRepository replyRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewReplyResponse create(UUID reviewId, UUID sellerId, CreateReviewReplyRequest request) {
        var review = reviewRepository.findByIdAndStatusAndDeletedAtIsNull(reviewId, ReviewStatus.APPROVED)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Review not found: " + reviewId));

        if (replyRepository.existsByReviewId(reviewId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Bu yoruma zaten yanit verilmis.");
        }

        var reply = ReviewReply.builder()
            .review(review)
            .sellerId(sellerId)
            .content(request.content())
            .build();

        return reviewMapper.toReplyResponse(replyRepository.save(reply));
    }

    @Transactional
    public ReviewReplyResponse update(UUID replyId, UUID sellerId, UpdateReviewReplyRequest request) {
        var reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Reply not found: " + replyId));

        if (!reply.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Bu yaniti duzenleme yetkiniz yok.");
        }

        reply.setContent(request.content());
        return reviewMapper.toReplyResponse(replyRepository.save(reply));
    }

    @Transactional
    public void delete(UUID replyId, UUID sellerId) {
        var reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Reply not found: " + replyId));

        if (!reply.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Bu yaniti silme yetkiniz yok.");
        }

        replyRepository.delete(reply);
    }

    @Transactional(readOnly = true)
    public List<ReviewReplyResponse> getMyReplies(UUID sellerId) {
        return replyRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
            .map(reviewMapper::toReplyResponse)
            .toList();
    }
}
