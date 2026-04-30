package com.smartcommerce.ai.service;

import com.smartcommerce.ai.repository.AiUsageLogRepository;
import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiBudgetGuard {

    private final AiUsageLogRepository repository;

    @Value("${ai.daily-budget-usd:10.00}")
    private BigDecimal dailyBudget;

    public void check() {
        var today = repository.sumCostUsdSince(Instant.now().truncatedTo(ChronoUnit.DAYS));
        if (today != null && today.compareTo(dailyBudget) >= 0) {
            log.warn("AI daily budget exceeded: spent={} budget={}", today, dailyBudget);
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Daily AI budget exceeded: $" + today + " / $" + dailyBudget);
        }
    }

    public BigDecimal todaySpend() {
        var spent = repository.sumCostUsdSince(Instant.now().truncatedTo(ChronoUnit.DAYS));
        return spent != null ? spent : BigDecimal.ZERO;
    }

    public BigDecimal remainingBudget() {
        return dailyBudget.subtract(todaySpend()).max(BigDecimal.ZERO);
    }

    public BigDecimal getDailyBudget() {
        return dailyBudget;
    }
}
