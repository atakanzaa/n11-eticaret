package com.smartcommerce.order.client;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class FraudDetectionClient {
    private static final BigDecimal FRAUD_THRESHOLD = new BigDecimal("50000");

    public FraudCheckResult check(FraudCheckRequest request) {
        if (request.amount().compareTo(FRAUD_THRESHOLD) > 0) {
            return new FraudCheckResult(true, "AMOUNT_THRESHOLD_EXCEEDED");
        }
        return new FraudCheckResult(false, null);
    }

    public record FraudCheckRequest(UUID orderId, UUID userId, BigDecimal amount, String currency, int itemCount) {}
    public record FraudCheckResult(boolean flagged, String reason) {}
}
