package com.smartcommerce.fraud.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmountVelocityRule implements FraudRule {

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("100000");

    @Override
    public String getName() {
        return "AMOUNT_VELOCITY";
    }

    @Override
    public RuleResult evaluate(FraudCheckContext ctx) {
        if (ctx.userAmountLast24h().compareTo(DAILY_LIMIT) > 0) {
            return new RuleResult(true, 60, "100k+ TRY spent in 24h");
        }
        return RuleResult.notTriggered();
    }
}
