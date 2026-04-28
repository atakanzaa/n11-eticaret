package com.smartcommerce.recommendation.job;

import com.smartcommerce.recommendation.domain.ProductPopularity;
import com.smartcommerce.recommendation.repository.ProductPopularityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularityScoreJob {

    private static final double WEIGHT_PURCHASE = 10.0;
    private static final double WEIGHT_CART_ADD = 3.0;
    private static final double WEIGHT_VIEW = 1.0;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductPopularityRepository popularityRepository;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void flushAndScore() {
        var keys = redisTemplate.keys("counter:product:*");
        if (keys == null || keys.isEmpty()) return;

        var byProduct = new HashMap<UUID, Map<String, Long>>();
        for (var key : keys) {
            var parts = key.split(":");
            if (parts.length < 4) continue;
            UUID pid;
            try {
                pid = UUID.fromString(parts[2]);
            } catch (IllegalArgumentException e) {
                continue;
            }
            var metric = parts[3];
            var val = redisTemplate.opsForValue().get(key);
            if (val == null) continue;
            byProduct.computeIfAbsent(pid, k -> new HashMap<>())
                .put(metric, Long.parseLong(val.toString()));
        }

        for (var entry : byProduct.entrySet()) {
            var pid = entry.getKey();
            var counters = entry.getValue();
            var pop = popularityRepository.findById(pid)
                .orElseGet(() -> ProductPopularity.builder()
                    .productId(pid).viewCount(0L).cartAddCount(0L).purchaseCount(0L)
                    .score(BigDecimal.ZERO).lastCalculatedAt(Instant.now()).build());
            pop.setViewCount(pop.getViewCount() + counters.getOrDefault("views", 0L));
            pop.setCartAddCount(pop.getCartAddCount() + counters.getOrDefault("cart-adds", 0L));
            pop.setPurchaseCount(pop.getPurchaseCount() + counters.getOrDefault("purchases", 0L));
            var score = pop.getPurchaseCount() * WEIGHT_PURCHASE
                + pop.getCartAddCount() * WEIGHT_CART_ADD
                + pop.getViewCount() * WEIGHT_VIEW;
            pop.setScore(BigDecimal.valueOf(score));
            pop.setLastCalculatedAt(Instant.now());
            popularityRepository.save(pop);

            for (var metric : counters.keySet()) {
                redisTemplate.delete("counter:product:" + pid + ":" + metric);
            }
        }
        log.info("Flushed popularity counters for {} products", byProduct.size());
    }
}
