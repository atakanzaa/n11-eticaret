package com.smartcommerce.ai.repository;

import com.smartcommerce.ai.domain.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {

    @Query("SELECT COALESCE(SUM(u.costUsd), 0) FROM AiUsageLog u WHERE u.createdAt >= :since")
    BigDecimal sumCostUsdSince(@Param("since") Instant since);

    /**
     * Returns rows of {model, request_count, total_cost_usd, total_input_tokens, total_output_tokens}
     * for usage logs created on/after :since. Provider is inferred from the model name on the
     * application side (claude-*, gpt-*, gemini-*).
     */
    @Query(value = """
        SELECT u.model AS model,
               COUNT(*) AS requestCount,
               COALESCE(SUM(u.cost_usd), 0) AS totalCost,
               COALESCE(SUM(u.input_tokens), 0) AS inputTokens,
               COALESCE(SUM(u.output_tokens), 0) AS outputTokens
        FROM ai_usage_log u
        WHERE u.created_at >= :since
        GROUP BY u.model
        ORDER BY totalCost DESC
        """, nativeQuery = true)
    List<Object[]> aggregateByModelSince(@Param("since") Instant since);

    @Query(value = """
        SELECT u.purpose AS purpose,
               COUNT(*) AS requestCount,
               COALESCE(SUM(u.cost_usd), 0) AS totalCost
        FROM ai_usage_log u
        WHERE u.created_at >= :since
        GROUP BY u.purpose
        ORDER BY requestCount DESC
        """, nativeQuery = true)
    List<Object[]> aggregateByPurposeSince(@Param("since") Instant since);

    @Query(value = """
        SELECT DATE_TRUNC('day', u.created_at) AS day,
               COALESCE(SUM(u.cost_usd), 0) AS totalCost,
               COUNT(*) AS requestCount
        FROM ai_usage_log u
        WHERE u.created_at >= :since
        GROUP BY DATE_TRUNC('day', u.created_at)
        ORDER BY day ASC
        """, nativeQuery = true)
    List<Object[]> dailySpendSince(@Param("since") Instant since);
}
