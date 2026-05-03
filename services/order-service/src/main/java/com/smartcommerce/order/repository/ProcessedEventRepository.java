package com.smartcommerce.order.repository;

import com.smartcommerce.order.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    /** Bulk-delete rows older than the cutoff. Used by ProcessedEventCleanupJob. */
    @Modifying
    @Query("delete from ProcessedEvent p where p.processedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
