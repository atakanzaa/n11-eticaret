package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.CreateReviewRequest;
import com.smartcommerce.product.api.dto.ReviewResponse;
import com.smartcommerce.product.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/products/{productId}/reviews")
    public Page<ReviewResponse> listByProduct(@PathVariable UUID productId, Pageable pageable) {
        return reviewService.listByProduct(productId, pageable);
    }

    @PostMapping("/api/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable UUID productId,
                                 @AuthenticationPrincipal String userId,
                                 @RequestHeader(value = "X-User-Display-Name", defaultValue = "Müşteri") String userDisplayName,
                                 @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(productId, UUID.fromString(userId), userDisplayName, request);
    }

    @GetMapping("/api/reviews/{id}")
    public ReviewResponse getById(@PathVariable UUID id) {
        return reviewService.getById(id);
    }

    @PostMapping("/api/reviews/{id}/helpful")
    public ReviewResponse markHelpful(@PathVariable UUID id) {
        return reviewService.markHelpful(id);
    }
}
