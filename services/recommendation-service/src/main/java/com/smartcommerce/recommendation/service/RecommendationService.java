package com.smartcommerce.recommendation.service;

import com.smartcommerce.recommendation.api.dto.ProductRecommendation;
import com.smartcommerce.recommendation.domain.UserBehavior;
import com.smartcommerce.recommendation.repository.CoPurchaseRepository;
import com.smartcommerce.recommendation.repository.ProductPopularityRepository;
import com.smartcommerce.recommendation.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserBehaviorRepository behaviorRepository;
    private final ProductPopularityRepository popularityRepository;
    private final CoPurchaseRepository coPurchaseRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<ProductRecommendation> getForUser(UUID userId, int limit) {
        var behaviors = behaviorRepository.findTop50ByUserIdOrderByOccurredAtDesc(userId);
        return behaviors.isEmpty() ? getPopular(limit) : getPersonalized(userId, behaviors, limit);
    }

    public List<ProductRecommendation> getPopular(int limit) {
        return popularityRepository.findTop50ByOrderByScoreDesc().stream()
            .limit(limit)
            .map(p -> new ProductRecommendation(p.getProductId(), p.getScore().doubleValue(), "POPULARITY"))
            .toList();
    }

    public List<ProductRecommendation> getRecentlyViewed(UUID userId, int limit) {
        var key = BehaviorTrackingService.RECENT_VIEWS_PREFIX + userId + ":recent-views";
        var ids = redisTemplate.opsForZSet().reverseRange(key, 0, Math.max(0, limit - 1));
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
            .map(id -> new ProductRecommendation(UUID.fromString(id.toString()), 1.0, "RECENTLY_VIEWED"))
            .toList();
    }

    public List<ProductRecommendation> getCoPurchased(UUID productId, int limit) {
        return coPurchaseRepository.findTop10ByProductAIdOrderByCoPurchaseCountDesc(productId).stream()
            .limit(limit)
            .map(p -> new ProductRecommendation(p.getProductBId(),
                (double) p.getCoPurchaseCount(), "FREQUENTLY_BOUGHT_TOGETHER"))
            .toList();
    }

    private List<ProductRecommendation> getPersonalized(UUID userId, List<UserBehavior> behaviors, int limit) {
        var purchased = behaviors.stream()
            .filter(b -> "PURCHASE".equals(b.getActionType()))
            .map(UserBehavior::getProductId).distinct().limit(10).toList();
        Set<UUID> seen = new HashSet<>();
        for (var b : behaviors) seen.add(b.getProductId());

        var recs = new ArrayList<ProductRecommendation>();
        for (var pid : purchased) recs.addAll(getCoPurchased(pid, 5));
        var filtered = recs.stream().filter(r -> !seen.contains(r.productId())).toList();
        var deduped = new ArrayList<ProductRecommendation>();
        var added = new HashSet<UUID>();
        for (var r : filtered) {
            if (added.add(r.productId())) deduped.add(r);
        }
        if (deduped.size() < limit) {
            getPopular(limit - deduped.size()).stream()
                .filter(r -> added.add(r.productId()))
                .forEach(deduped::add);
        }
        return deduped.stream().limit(limit).toList();
    }
}
