package com.smartcommerce.fraud.service;

import org.springframework.stereotype.Component;

@Component
public class VelocityRule implements FraudRule {

    @Override
    public String getName() {
        return "VELOCITY";
    }

    @Override
    public RuleResult evaluate(FraudCheckContext ctx) {
        if (ctx.userOrdersLastHour() >= 5) {
            return new RuleResult(true, 70, "5+ orders in last hour");
        }
        if (ctx.userOrdersLast24h() >= 15) {
            return new RuleResult(true, 50, "15+ orders in last 24 hours");
        }
        return RuleResult.notTriggered();
    }
}
