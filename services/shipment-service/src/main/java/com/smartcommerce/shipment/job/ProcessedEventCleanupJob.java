package com.smartcommerce.shipment.job;

import com.smartcommerce.shipment.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Prunes processed_events rows older than 30 days to keep idempotency tables
 * bounded. Runs daily at 03:30 server time.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventCleanupJob {

    private static final Duration RETENTION = Duration.ofDays(30);

    private final ProcessedEventRepository processedEventRepository;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanup() {
        var cutoff = Instant.now().minus(RETENTION);
        var deleted = processedEventRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("[shipment] Pruned {} processed_events rows older than {}", deleted, cutoff);
        }
    }
}
