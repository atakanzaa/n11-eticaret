package com.smartcommerce.fraud.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighAmountRule implements FraudRule {

    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("20000");

    @Override
    public String getName() {
        return "HIGH_AMOUNT";
    }

    @Override
    public RuleResult evaluate(FraudCheckContext ctx) {
        if (ctx.amount().compareTo(CRITICAL_THRESHOLD) > 0) {
            return new RuleResult(true, 80, "Order amount exceeds 50,000 TRY");
        }
        if (ctx.amount().compareTo(WARNING_THRESHOLD) > 0) {
            return new RuleResult(true, 30, "Order amount exceeds 20,000 TRY");
        }
        return RuleResult.notTriggered();
    }
}
