package com.smartcommerce.fraud.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighAmountRule implements FraudRule {

    // Premium electronics (iPhone, Samsung Galaxy S24, gaming laptops) cost
    // ₺50–80K in 2026 Turkey. The original ₺50K cutoff blocked normal carts
    // with a single phone. Pushed to ₺250K so only genuinely outlier baskets
    // — multiple high-end items — trigger the critical decline.
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("250000");
    private static final BigDecimal WARNING_THRESHOLD  = new BigDecimal("100000");

    @Override
    public String getName() {
        return "HIGH_AMOUNT";
    }

    @Override
    public RuleResult evaluate(FraudCheckContext ctx) {
        if (ctx.amount().compareTo(CRITICAL_THRESHOLD) > 0) {
            return new RuleResult(true, 80, "Order amount exceeds 250,000 TRY");
        }
        if (ctx.amount().compareTo(WARNING_THRESHOLD) > 0) {
            return new RuleResult(true, 30, "Order amount exceeds 100,000 TRY");
        }
        return RuleResult.notTriggered();
    }
}
