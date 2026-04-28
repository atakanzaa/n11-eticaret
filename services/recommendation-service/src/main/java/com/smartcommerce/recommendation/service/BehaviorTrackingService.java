package com.smartcommerce.recommendation.service;

import com.smartcommerce.recommendation.domain.CoPurchasePair;
import com.smartcommerce.recommendation.domain.UserBehavior;
import com.smartcommerce.recommendation.repository.CoPurchaseRepository;
import com.smartcommerce.recommendation.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BehaviorTrackingService {

    static final String COUNTER_PREFIX = "counter:product:";
    static final String RECENT_VIEWS_PREFIX = "behavior:user:";

    private final UserBehaviorRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CoPurchaseRepository coPurchaseRepository;

    public void trackView(UUID userId, UUID productId) {
        track(userId, productId, "VIEW");
        var key = RECENT_VIEWS_PREFIX + userId + ":recent-views";
        redisTemplate.opsForZSet().add(key, productId.toString(), System.currentTimeMillis());
        redisTemplate.opsForZSet().removeRange(key, 0, -51);
        redisTemplate.expire(key, Duration.ofDays(30));
        redisTemplate.opsForValue().increment(COUNTER_PREFIX + productId + ":views");
    }

    public void trackCartAdd(UUID userId, UUID productId) {
        track(userId, productId, "CART_ADD");
        redisTemplate.opsForValue().increment(COUNTER_PREFIX + productId + ":cart-adds");
    }

    @Transactional
    public void trackPurchase(UUID userId, List<UUID> productIds) {
        for (var pid : productIds) {
            track(userId, pid, "PURCHASE");
            redisTemplate.opsForValue().increment(COUNTER_PREFIX + pid + ":purchases");
        }
        for (var i = 0; i < productIds.size(); i++) {
            for (var j = 0; j < productIds.size(); j++) {
                if (i == j) continue;
                upsertCoPurchase(productIds.get(i), productIds.get(j));
            }
        }
    }

    private void track(UUID userId, UUID productId, String action) {
        repository.save(UserBehavior.builder()
            .userId(userId).productId(productId).actionType(action)
            .occurredAt(Instant.now()).build());
    }

    private void upsertCoPurchase(UUID a, UUID b) {
        var pair = coPurchaseRepository.findByProductAIdAndProductBId(a, b)
            .orElseGet(() -> CoPurchasePair.builder()
                .productAId(a).productBId(b).coPurchaseCount(0)
                .lastSeenAt(Instant.now()).build());
        pair.setCoPurchaseCount(pair.getCoPurchaseCount() + 1);
        pair.setLastSeenAt(Instant.now());
        coPurchaseRepository.save(pair);
    }
}
