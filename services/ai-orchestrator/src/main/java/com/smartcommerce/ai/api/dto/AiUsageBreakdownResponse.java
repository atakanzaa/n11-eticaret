package com.smartcommerce.ai.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AiUsageBreakdownResponse(
    List<ProviderUsage> byProvider,
    List<PurposeUsage> byPurpose,
    List<DailyUsage> dailyUsage,
    BigDecimal dailyBudgetUsd,
    BigDecimal todayUsedUsd,
    BigDecimal remainingUsd
) {
    public record ProviderUsage(
        String provider,
        long requestCount,
        BigDecimal costUsd,
        long inputTokens,
        long outputTokens
    ) {
    }

    public record PurposeUsage(
        String purpose,
        long requestCount,
        BigDecimal costUsd
    ) {
    }

    public record DailyUsage(
        LocalDate date,
        BigDecimal costUsd,
        long requestCount
    ) {
    }
}
