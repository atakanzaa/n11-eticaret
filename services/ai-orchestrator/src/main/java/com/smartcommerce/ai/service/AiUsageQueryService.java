package com.smartcommerce.ai.service;

import com.smartcommerce.ai.api.dto.AiUsageBreakdownResponse;
import com.smartcommerce.ai.api.dto.AiUsageBreakdownResponse.DailyUsage;
import com.smartcommerce.ai.api.dto.AiUsageBreakdownResponse.ProviderUsage;
import com.smartcommerce.ai.api.dto.AiUsageBreakdownResponse.PurposeUsage;
import com.smartcommerce.ai.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregates rows from `ai_usage_log` into the shape the admin AI usage screen
 * expects: provider donut, purpose table, daily spend line, and budget meter.
 *
 * Provider is derived from model prefix because we never persisted the provider
 * name explicitly (the AbstractAiAdapter pattern carries it implicitly).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiUsageQueryService {

    private final AiUsageLogRepository repository;
    private final AiBudgetGuard budgetGuard;

    public AiUsageBreakdownResponse breakdown(int days) {
        var since = Instant.now().minus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        return new AiUsageBreakdownResponse(
            providerBreakdown(since),
            purposeBreakdown(since),
            dailyBreakdown(since, days),
            budgetGuard.getDailyBudget(),
            budgetGuard.todaySpend(),
            budgetGuard.remainingBudget()
        );
    }

    private List<ProviderUsage> providerBreakdown(Instant since) {
        // Roll up the per-model rows into per-provider rows.
        Map<String, ProviderAccumulator> agg = new HashMap<>();
        for (Object[] row : repository.aggregateByModelSince(since)) {
            String model = (String) row[0];
            String provider = providerFor(model);
            agg.computeIfAbsent(provider, p -> new ProviderAccumulator())
                .add((Number) row[1], (Number) row[2], (Number) row[3], (Number) row[4]);
        }
        return agg.entrySet().stream()
            .map(e -> new ProviderUsage(e.getKey(), e.getValue().requestCount,
                e.getValue().costUsd, e.getValue().inputTokens, e.getValue().outputTokens))
            .sorted((a, b) -> b.costUsd().compareTo(a.costUsd()))
            .toList();
    }

    private List<PurposeUsage> purposeBreakdown(Instant since) {
        return repository.aggregateByPurposeSince(since).stream()
            .map(row -> new PurposeUsage(
                (String) row[0],
                ((Number) row[1]).longValue(),
                toBigDecimal(row[2])))
            .toList();
    }

    private List<DailyUsage> dailyBreakdown(Instant since, int days) {
        // Bucket by date. Fill in zero-rows for days with no requests so the chart's
        // x-axis is dense.
        var rows = repository.dailySpendSince(since);
        var byDay = new TreeMap<LocalDate, DailyUsage>();
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            byDay.put(day, new DailyUsage(day, toBigDecimal(row[1]), ((Number) row[2]).longValue()));
        }
        var today = LocalDate.now(ZoneOffset.UTC);
        for (int i = days - 1; i >= 0; i--) {
            var d = today.minusDays(i);
            byDay.putIfAbsent(d, new DailyUsage(d, BigDecimal.ZERO, 0));
        }
        return byDay.values().stream().toList();
    }

    /**
     * The dailySpendSince native query returns the bucketed timestamp as
     * Timestamp on some drivers and as Instant on others (Hibernate 6 +
     * pgjdbc with java.time mapping). Accept both, plus java.sql.Date for
     * date_trunc('day', ...) edge cases.
     */
    private static LocalDate toLocalDate(Object value) {
        if (value == null) throw new IllegalStateException("daily bucket value is null");
        if (value instanceof Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        if (value instanceof Instant in) return in.atOffset(ZoneOffset.UTC).toLocalDate();
        if (value instanceof java.sql.Date sd) return sd.toLocalDate();
        if (value instanceof java.time.LocalDate ld) return ld;
        if (value instanceof java.time.LocalDateTime ldt) return ldt.toLocalDate();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toLocalDate();
        throw new IllegalStateException("Unsupported daily bucket type: " + value.getClass().getName());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private static String providerFor(String model) {
        if (model == null) return "unknown";
        var lower = model.toLowerCase();
        if (lower.startsWith("claude")) return "anthropic";
        if (lower.startsWith("gpt")) return "openai";
        if (lower.startsWith("gemini")) return "gemini";
        return "other";
    }

    private static final class ProviderAccumulator {
        long requestCount;
        BigDecimal costUsd = BigDecimal.ZERO;
        long inputTokens;
        long outputTokens;

        void add(Number reqCount, Number cost, Number inputTok, Number outputTok) {
            this.requestCount += reqCount.longValue();
            this.costUsd = this.costUsd.add(toBigDecimal(cost));
            this.inputTokens += inputTok.longValue();
            this.outputTokens += outputTok.longValue();
        }
    }
}
