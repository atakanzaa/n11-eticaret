package com.smartcommerce.cart.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@FeignClient(name = "promotion-service", url = "${services.promotion.url:http://localhost:8093}")
public interface PromotionClient {

    @GetMapping("/api/campaigns/active-by-offer/{offerId}")
    @CircuitBreaker(name = "promotion-service")
    ActiveCampaign activeCampaign(@PathVariable UUID offerId);

    record ActiveCampaign(UUID id, UUID offerId, UUID sellerId, UUID productId,
                          BigDecimal discountedPrice, Instant startsAt, Instant endsAt,
                          boolean active) {
    }
}
