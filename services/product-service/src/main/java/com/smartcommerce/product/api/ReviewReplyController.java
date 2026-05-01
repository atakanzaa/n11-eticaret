package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.CreateReviewReplyRequest;
import com.smartcommerce.product.api.dto.ReviewReplyResponse;
import com.smartcommerce.product.api.dto.UpdateReviewReplyRequest;
import com.smartcommerce.product.service.ReviewReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewReplyController {
    private final ReviewReplyService replyService;

    @PostMapping("/api/reviews/{reviewId}/reply")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewReplyResponse create(@PathVariable UUID reviewId,
                                      @RequestHeader("X-Seller-Id") UUID sellerId,
                                      @Valid @RequestBody CreateReviewReplyRequest request) {
        return replyService.create(reviewId, sellerId, request);
    }

    @PutMapping("/api/reviews/replies/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ReviewReplyResponse update(@PathVariable UUID id,
                                      @RequestHeader("X-Seller-Id") UUID sellerId,
                                      @Valid @RequestBody UpdateReviewReplyRequest request) {
        return replyService.update(id, sellerId, request);
    }

    @DeleteMapping("/api/reviews/replies/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @RequestHeader("X-Seller-Id") UUID sellerId) {
        replyService.delete(id, sellerId);
    }

    @GetMapping("/api/reviews/replies/my")
    @PreAuthorize("hasRole('SELLER')")
    public List<ReviewReplyResponse> getMyReplies(@RequestHeader("X-Seller-Id") UUID sellerId) {
        return replyService.getMyReplies(sellerId);
    }
}
