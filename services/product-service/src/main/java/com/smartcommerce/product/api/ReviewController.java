package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.*;
import com.smartcommerce.product.domain.VoteType;
import com.smartcommerce.product.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/api/products/{productId}/reviews")
    public Page<ReviewResponse> listByProduct(@PathVariable UUID productId,
                                               @RequestParam(required = false) Integer rating,
                                               @RequestParam(required = false) Boolean verifiedOnly,
                                               @RequestParam(required = false) Instant fromDate,
                                               @RequestParam(required = false) Instant toDate,
                                               Pageable pageable) {
        if (rating != null || verifiedOnly != null || fromDate != null || toDate != null) {
            return reviewService.listFiltered(productId, rating, verifiedOnly, fromDate, toDate, pageable);
        }
        return reviewService.listByProduct(productId, pageable);
    }

    @GetMapping("/api/products/{productId}/reviews/stats")
    public ReviewStatsResponse getStats(@PathVariable UUID productId) {
        return reviewService.getStats(productId);
    }

    @GetMapping("/api/products/{productId}/reviews/me")
    @PreAuthorize("isAuthenticated()")
    public org.springframework.http.ResponseEntity<ReviewResponse> getMine(@PathVariable UUID productId,
                                                                            @AuthenticationPrincipal String userId) {
        return reviewService.findMineForProduct(productId, UUID.fromString(userId))
            .map(org.springframework.http.ResponseEntity::ok)
            .orElseGet(() -> org.springframework.http.ResponseEntity.<ReviewResponse>noContent().build());
    }

    @GetMapping("/api/reviews/my")
    @PreAuthorize("isAuthenticated()")
    public Page<ReviewResponse> listMyReviews(@AuthenticationPrincipal String userId,
                                              Pageable pageable) {
        return reviewService.listMyReviews(UUID.fromString(userId), pageable);
    }

    @PostMapping("/api/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable UUID productId,
                                 @AuthenticationPrincipal String userId,
                                 @RequestHeader(value = "X-User-Display-Name", defaultValue = "Musteri") String userDisplayName,
                                 @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(productId, UUID.fromString(userId), userDisplayName, request);
    }

    @GetMapping("/api/reviews/{id}")
    public ReviewResponse getById(@PathVariable UUID id) {
        return reviewService.getById(id);
    }

    @PutMapping("/api/reviews/{id}")
    public ReviewResponse update(@PathVariable UUID id,
                                 @AuthenticationPrincipal String userId,
                                 @Valid @RequestBody UpdateReviewRequest request) {
        return reviewService.update(id, UUID.fromString(userId), request);
    }

    @DeleteMapping("/api/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @AuthenticationPrincipal String userId,
                       Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        reviewService.delete(id, UUID.fromString(userId), isAdmin);
    }

    @PostMapping("/api/reviews/{id}/vote")
    public ReviewResponse vote(@PathVariable UUID id,
                               @AuthenticationPrincipal String userId,
                               @RequestParam VoteType voteType) {
        return reviewService.vote(id, UUID.fromString(userId), voteType);
    }

    @PostMapping("/api/reviews/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewReportResponse report(@PathVariable UUID id,
                                       @AuthenticationPrincipal String userId,
                                       @Valid @RequestBody CreateReviewReportRequest request) {
        return reviewService.report(id, UUID.fromString(userId), request);
    }
}
