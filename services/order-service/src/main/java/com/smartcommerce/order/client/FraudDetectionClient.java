package com.smartcommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "fraud-detection-service", url = "${services.fraud.url:http://localhost:8095}",
    fallback = FraudDetectionClient.Fallback.class)
public interface FraudDetectionClient {

    @PostMapping("/api/fraud/check")
    @CircuitBreaker(name = "fraud-detection", fallbackMethod = "checkFallback")
    FraudCheckResult check(@RequestBody FraudCheckRequest request);

    default FraudCheckResult checkFallback(FraudCheckRequest request, Throwable t) {
        // Fail-open: if fraud-service is down, allow the order to proceed.
        // Trade-off documented in ADR — better UX than blocking on dependency outage.
        // Operations alerted via circuit-breaker open metric.
        return new FraudCheckResult(false, "APPROVED_ON_FALLBACK", 0,
            "fraud-detection-service unavailable", List.of());
    }

    record FraudCheckRequest(
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        int itemCount,
        String userIp,
        boolean firstOrder
    ) {}

    record FraudCheckResult(
        boolean flagged,
        String decision,
        int riskScore,
        String reason,
        List<String> triggeredRules
    ) {}

    @Slf4j
    @org.springframework.stereotype.Component
    class Fallback implements FraudDetectionClient {
        @Override
        public FraudCheckResult check(FraudCheckRequest request) {
            log.warn("fraud-detection-service unavailable for order {}, falling back to APPROVED", request.orderId());
            return new FraudCheckResult(false, "APPROVED_ON_FALLBACK", 0,
                "fraud-detection-service unavailable", List.of());
        }
    }
}
