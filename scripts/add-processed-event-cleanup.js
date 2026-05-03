#!/usr/bin/env node
/**
 * Adds a daily cleanup job that prunes processed_events rows older than 30 days
 * to every service that has a ProcessedEventRepository. Idempotent.
 */
const fs = require('fs');
const path = require('path');

const services = [
  ['cart-service',          'com.smartcommerce.cart',           'cart',           'cart'],
  ['inventory-service',     'com.smartcommerce.inventory',      'inventory',      'inventory'],
  ['notification-service',  'com.smartcommerce.notification',   'notification',   'notification'],
  ['order-service',         'com.smartcommerce.order',          'order',          'order'],
  ['payment-service',       'com.smartcommerce.payment',        'payment',        'payment'],
  ['promotion-service',     'com.smartcommerce.promotion',      'promotion',      'promotion'],
  ['recommendation-service','com.smartcommerce.recommendation', 'recommendation', 'recommendation'],
  ['return-service',        'com.smartcommerce.returns',        'returns',        'return'],
  ['seller-service',        'com.smartcommerce.seller',         'seller',         'seller'],
  ['shipment-service',      'com.smartcommerce.shipment',       'shipment',       'shipment'],
  ['user-service',          'com.smartcommerce.user',           'user',           'user'],
];

function repoTemplate(pkg) {
  return `package ${pkg}.repository;

import ${pkg}.domain.ProcessedEvent;
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
`;
}

function jobTemplate(pkg, label) {
  return `package ${pkg}.job;

import ${pkg}.repository.ProcessedEventRepository;
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
            log.info("[${label}] Pruned {} processed_events rows older than {}", deleted, cutoff);
        }
    }
}
`;
}

let touched = 0;
for (const [svc, pkg, pkgPath, label] of services) {
  const javaRoot = path.join('services', svc, 'src', 'main', 'java', ...pkg.split('.'));
  const repoPath = path.join(javaRoot, 'repository', 'ProcessedEventRepository.java');
  if (!fs.existsSync(repoPath)) {
    console.log(`SKIP ${svc}: no ProcessedEventRepository`);
    continue;
  }
  // Update repo
  fs.writeFileSync(repoPath, repoTemplate(pkg));
  // Create job dir + file
  const jobDir = path.join(javaRoot, 'job');
  fs.mkdirSync(jobDir, { recursive: true });
  const jobPath = path.join(jobDir, 'ProcessedEventCleanupJob.java');
  fs.writeFileSync(jobPath, jobTemplate(pkg, label));
  touched++;
  console.log(`OK ${svc}`);
}
console.log(`${touched} services updated`);
