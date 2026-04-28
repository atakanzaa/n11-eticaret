package com.smartcommerce.recommendation.api;

import com.smartcommerce.recommendation.api.dto.ProductRecommendation;
import com.smartcommerce.recommendation.service.BehaviorTrackingService;
import com.smartcommerce.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final BehaviorTrackingService trackingService;

    @GetMapping("/me")
    public List<ProductRecommendation> forMe(@AuthenticationPrincipal String userId,
                                             @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getForUser(UUID.fromString(userId), limit);
    }

    @GetMapping("/popular")
    public List<ProductRecommendation> popular(@RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getPopular(limit);
    }

    @GetMapping("/recently-viewed")
    public List<ProductRecommendation> recentlyViewed(@AuthenticationPrincipal String userId,
                                                      @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getRecentlyViewed(UUID.fromString(userId), limit);
    }

    @GetMapping("/products/{id}/related")
    public List<ProductRecommendation> related(@PathVariable("id") UUID productId,
                                               @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getCoPurchased(productId, limit);
    }

    @PostMapping("/track/view")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void trackView(@AuthenticationPrincipal String userId, @RequestParam UUID productId) {
        trackingService.trackView(UUID.fromString(userId), productId);
    }

    @GetMapping("/internal/users/{userId}")
    public List<ProductRecommendation> getForUserInternal(@PathVariable UUID userId,
                                                          @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getForUser(userId, limit);
    }
}
