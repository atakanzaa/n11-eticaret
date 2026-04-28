package com.smartcommerce.recommendation.service;

import com.smartcommerce.recommendation.domain.CoPurchasePair;
import com.smartcommerce.recommendation.domain.ProductPopularity;
import com.smartcommerce.recommendation.repository.CoPurchaseRepository;
import com.smartcommerce.recommendation.repository.ProductPopularityRepository;
import com.smartcommerce.recommendation.repository.UserBehaviorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock UserBehaviorRepository behaviorRepository;
    @Mock ProductPopularityRepository popularityRepository;
    @Mock CoPurchaseRepository coPurchaseRepository;
    @Mock RedisTemplate<String, Object> redisTemplate;

    RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(behaviorRepository, popularityRepository,
            coPurchaseRepository, redisTemplate);
    }

    @Test
    @DisplayName("popular returns top products sorted by score")
    void popular_returnsByScoreDesc() {
        var p1 = ProductPopularity.builder().productId(UUID.randomUUID()).score(new BigDecimal("100.00")).build();
        var p2 = ProductPopularity.builder().productId(UUID.randomUUID()).score(new BigDecimal("50.00")).build();
        when(popularityRepository.findTop50ByOrderByScoreDesc()).thenReturn(List.of(p1, p2));

        var result = service.getPopular(5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).reason()).isEqualTo("POPULARITY");
        assertThat(result.get(0).score()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("co-purchased products returned as FREQUENTLY_BOUGHT_TOGETHER")
    void coPurchased_marksReason() {
        var productId = UUID.randomUUID();
        var partner = UUID.randomUUID();
        var pair = CoPurchasePair.builder()
            .productAId(productId).productBId(partner).coPurchaseCount(7)
            .lastSeenAt(Instant.now()).build();
        when(coPurchaseRepository.findTop10ByProductAIdOrderByCoPurchaseCountDesc(productId))
            .thenReturn(List.of(pair));

        var result = service.getCoPurchased(productId, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo(partner);
        assertThat(result.get(0).reason()).isEqualTo("FREQUENTLY_BOUGHT_TOGETHER");
        assertThat(result.get(0).score()).isEqualTo(7.0);
    }

    @Test
    @DisplayName("user with no behavior history falls back to popular recommendations")
    void forUser_emptyHistoryFallsBackToPopular() {
        var userId = UUID.randomUUID();
        when(behaviorRepository.findTop50ByUserIdOrderByOccurredAtDesc(userId)).thenReturn(List.of());
        var pop = ProductPopularity.builder().productId(UUID.randomUUID()).score(new BigDecimal("10.00")).build();
        when(popularityRepository.findTop50ByOrderByScoreDesc()).thenReturn(List.of(pop));

        var result = service.getForUser(userId, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).reason()).isEqualTo("POPULARITY");
    }
}
