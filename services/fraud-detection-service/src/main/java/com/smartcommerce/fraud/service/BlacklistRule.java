package com.smartcommerce.fraud.service;

import com.smartcommerce.fraud.repository.FraudBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlacklistRule implements FraudRule {

    private final FraudBlacklistRepository blacklistRepository;

    @Override
    public String getName() {
        return "BLACKLIST";
    }

    @Override
    public RuleResult evaluate(FraudCheckContext ctx) {
        if (blacklistRepository.existsByBlacklistTypeAndValue("USER_ID", ctx.userId().toString())) {
            return new RuleResult(true, 100, "User on blacklist");
        }
        if (ctx.userEmail() != null && blacklistRepository.existsByBlacklistTypeAndValue("EMAIL", ctx.userEmail())) {
            return new RuleResult(true, 100, "Email on blacklist");
        }
        if (ctx.userIp() != null && blacklistRepository.existsByBlacklistTypeAndValue("IP", ctx.userIp())) {
            return new RuleResult(true, 100, "IP on blacklist");
        }
        return RuleResult.notTriggered();
    }
}
