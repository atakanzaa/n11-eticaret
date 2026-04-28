package com.smartcommerce.fraud.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Component
public class NewUserHighAmountRule implements FraudRule {

    private static final BigDecimal NEW_USER_AMOUNT_LIMIT = new BigDecimal("5000");
    private static final long NEW_USER_AGE_HOURS = 24;

    @Override
    public String getName() {
        return "NEW_USER_HIGH_AMOUNT";
    }

    @Override
    public RuleResult evaluate(FraudCheckContext ctx) {
        if (ctx.userRegisteredAt() == null) return RuleResult.notTriggered();
        var ageHours = Duration.between(ctx.userRegisteredAt(), Instant.now()).toHours();
        if (ageHours < NEW_USER_AGE_HOURS && ctx.amount().compareTo(NEW_USER_AMOUNT_LIMIT) > 0) {
            return new RuleResult(true, 40, "New user (<24h) placing high-value order");
        }
        return RuleResult.notTriggered();
    }
}
