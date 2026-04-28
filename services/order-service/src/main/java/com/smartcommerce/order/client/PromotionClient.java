package com.smartcommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "promotion-service", url = "${services.promotion.url:http://localhost:8093}")
public interface PromotionClient {

    @PostMapping("/api/coupons/validate")
    @CircuitBreaker(name = "promotion-service")
    ValidationResult validate(@RequestBody ValidationRequest request);

    @PostMapping("/api/coupons/internal/apply")
    @CircuitBreaker(name = "promotion-service")
    Object apply(@RequestBody ApplyRequest request);

    record ValidationRequest(
        String code,
        UUID userId,
        BigDecimal cartTotal,
        BigDecimal shippingCost,
        List<UUID> productIds,
        List<UUID> categoryIds,
        List<UUID> sellerIds,
        boolean firstOrder
    ) {}

    record ValidationResult(
        boolean valid,
        String code,
        String discountType,
        BigDecimal discountAmount,
        String errorCode,
        String errorMessage
    ) {}

    record ApplyRequest(
        String code,
        UUID userId,
        UUID orderId,
        BigDecimal discountAmount
    ) {}
}
