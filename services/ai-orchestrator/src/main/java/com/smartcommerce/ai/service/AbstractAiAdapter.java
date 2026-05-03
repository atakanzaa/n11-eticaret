package com.smartcommerce.ai.service;

import com.smartcommerce.ai.domain.AiUsageLog;
import com.smartcommerce.ai.repository.AiUsageLogRepository;
import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Common boilerplate for adapters: budget guard, usage logging, cost calc.
 * Adapters subclass and implement {@link #invoke(AiRequest)} for the raw API call.
 */
@Slf4j
public abstract class AbstractAiAdapter implements AiProvider {

    protected final AiUsageLogRepository usageRepository;
    protected final AiBudgetGuard budgetGuard;

    protected AbstractAiAdapter(AiUsageLogRepository usageRepository, AiBudgetGuard budgetGuard) {
        this.usageRepository = usageRepository;
        this.budgetGuard = budgetGuard;
    }

    /** Per-million-token pricing for the given model. Adapters override with provider tables. */
    protected abstract Pricing pricingFor(String model);

    /** Currently configured model. */
    public abstract String model();

    /** Raw API call returning normalized response WITHOUT cost (cost computed by template). */
    protected abstract RawResult invoke(AiRequest request);

    @Override
    public AiResponse chat(AiRequest request) {
        budgetGuard.check();

        var start = Instant.now();
        try {
            var raw = invoke(request);
            var duration = Duration.between(start, Instant.now()).toMillis();
            var cost = calculateCost(raw.inputTokens(), raw.outputTokens());

            usageRepository.save(AiUsageLog.builder()
                .userId(request.userId())
                .purpose(request.purpose())
                .model(model())
                .inputTokens(raw.inputTokens())
                .outputTokens(raw.outputTokens())
                .costUsd(cost)
                .durationMs((int) duration)
                .success(true)
                .build());

            return new AiResponse(raw.content(), raw.stopReason(),
                raw.inputTokens(), raw.outputTokens(), cost);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} API call failed", getName(), e);
            usageRepository.save(AiUsageLog.builder()
                .userId(request.userId())
                .purpose(request.purpose())
                .model(model())
                .durationMs((int) Duration.between(start, Instant.now()).toMillis())
                .success(false)
                .errorMessage(e.getMessage())
                .build());
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "AI service unavailable (" + getName() + "): " + e.getMessage());
        }
    }

    private BigDecimal calculateCost(int inputTokens, int outputTokens) {
        var pricing = pricingFor(model());
        var perMillion = BigDecimal.valueOf(1_000_000);
        var inputCost = pricing.inputPerMillion().multiply(BigDecimal.valueOf(inputTokens))
            .divide(perMillion, 6, RoundingMode.HALF_UP);
        var outputCost = pricing.outputPerMillion().multiply(BigDecimal.valueOf(outputTokens))
            .divide(perMillion, 6, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    protected record RawResult(
        java.util.List<AiContentBlock> content,
        String stopReason,
        int inputTokens,
        int outputTokens
    ) {}

    /** USD per 1M tokens. */
    public record Pricing(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {
        public static Pricing of(double input, double output) {
            return new Pricing(BigDecimal.valueOf(input), BigDecimal.valueOf(output));
        }

        public static Pricing zero() {
            return new Pricing(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    protected static <T> T requireSuccess(T value, String description) {
        if (value == null) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "AI provider returned no " + description);
        }
        return value;
    }

    @SuppressWarnings("unused")
    protected static Map<String, Object> emptyIfNull(Map<String, Object> m) {
        return m == null ? Map.of() : m;
    }
}
