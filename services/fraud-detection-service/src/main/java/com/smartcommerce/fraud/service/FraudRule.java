package com.smartcommerce.fraud.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface FraudRule {

    RuleResult evaluate(FraudCheckContext ctx);

    String getName();

    record RuleResult(boolean triggered, int riskScore, String reason) {
        public static RuleResult notTriggered() {
            return new RuleResult(false, 0, null);
        }
    }

    record FraudCheckContext(
        UUID orderId,
        UUID userId,
        String userEmail,
        String userIp,
        BigDecimal amount,
        String currency,
        int itemCount,
        int userOrdersLast24h,
        BigDecimal userAmountLast24h,
        int userOrdersLastHour,
        boolean isFirstOrder,
        Instant userRegisteredAt
    ) {}
}
