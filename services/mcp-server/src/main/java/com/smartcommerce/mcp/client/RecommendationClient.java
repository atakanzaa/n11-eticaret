package com.smartcommerce.mcp.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "recommendation-service", url = "${services.recommendation.url:http://localhost:8087}")
public interface RecommendationClient {

    @GetMapping("/api/recommendations/internal/users/{userId}")
    @CircuitBreaker(name = "recommendation-service")
    Object getRecommendations(@PathVariable UUID userId, @RequestParam(defaultValue = "10") int limit);
}
