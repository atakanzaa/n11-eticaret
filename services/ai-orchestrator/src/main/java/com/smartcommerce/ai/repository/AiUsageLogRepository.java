package com.smartcommerce.ai.repository;

import com.smartcommerce.ai.domain.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {

    @Query("SELECT COALESCE(SUM(u.costUsd), 0) FROM AiUsageLog u WHERE u.createdAt >= :since")
    BigDecimal sumCostUsdSince(@Param("since") Instant since);
}
