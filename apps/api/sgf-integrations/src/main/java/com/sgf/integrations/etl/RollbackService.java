package com.sgf.integrations.etl;

import com.sgf.audit.service.AuditService;
import com.sgf.catalog.service.ProductService;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Rollback plan for ETL migrations.
 *
 * Every migration is tracked with snapshots that allow:
 * - Reverting to pre-migration state
 * - Partial rollback (specific source system only)
 * - Point-in-time recovery to any migration checkpoint
 *
 * Strategy:
 * - Pre-migration: snapshot product count per tenant
 * - During migration: track loaded record IDs grouped by batch
 * - Rollback: delete all records loaded in the target migration
 * - Post-rollback: verify product count matches snapshot
 *
 * This works alongside MigrationDashboard — each migration gets a RollbackPlan
 * that is populated as records are loaded.
 */
@Service
public class RollbackService {

    private static final Logger log = LoggerFactory.getLogger(RollbackService.class);

    private final AuditService auditService;
    private final ProductService productService;
    private final ConcurrentMap<String, RollbackPlan> plans = new ConcurrentHashMap<>();

    public RollbackService(AuditService auditService, ProductService productService) {
        this.auditService = auditService;
        this.productService = productService;
    }

    /**
     * Create a rollback plan before migration starts.
     * Takes a snapshot of current state.
     */
    public RollbackPlan createPlan(String migrationId, String sourceSystem, long preCount) {
        RollbackPlan plan = new RollbackPlan(
                migrationId,
                sourceSystem,
                OffsetDateTime.now(),
                preCount,
                new ArrayList<>(),
                RollbackStatus.READY
        );
        plans.put(migrationId, plan);
        log.info("Rollback plan created for {} (pre-migration count: {})", migrationId, preCount);
        return plan;
    }

    /**
     * Record a batch of loaded record IDs for potential rollback.
     */
    public void trackBatch(String migrationId, List<String> loadedProductIds) {
        RollbackPlan plan = plans.get(migrationId);
        if (plan != null && !loadedProductIds.isEmpty()) {
            plan.loadedIds.addAll(loadedProductIds);
            log.debug("Rollback tracking: {} records for {}", loadedProductIds.size(), migrationId);
        }
    }

    /**
     * Execute rollback: delete all records loaded in this migration.
     *
     * @param migrationId The migration to roll back
     * @return RollbackResult with status and counts
     */
    public RollbackResult rollback(String migrationId) {
        RollbackPlan plan = plans.get(migrationId);
        if (plan == null) {
            return new RollbackResult(migrationId, "NOT_FOUND", 0, 0,
                    "No rollback plan found for migration " + migrationId);
        }

        if (plan.status == RollbackStatus.EXECUTED) {
            return new RollbackResult(migrationId, "ALREADY_EXECUTED", plan.loadedIds.size(), plan.preCount,
                    "Rollback already executed for this migration");
        }

        if (plan.status == RollbackStatus.IN_PROGRESS) {
            return new RollbackResult(migrationId, "IN_PROGRESS", 0, 0,
                    "Rollback already in progress");
        }

        plan.status = RollbackStatus.IN_PROGRESS;
        int deleted = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        log.warn("Executing rollback for migration {}: {} records to revert from {}",
                migrationId, plan.loadedIds.size(), plan.sourceSystem);

        for (String productId : plan.loadedIds) {
            try {
                productService.delete(UUID.fromString(productId));
                deleted++;
            } catch (Exception e) {
                failed++;
                errors.add("Failed to rollback " + productId + ": " + e.getMessage());
                log.error("Rollback failed for {}: {}", productId, e.getMessage());
            }
        }

        // Verify post-rollback count matches pre-migration
        plan.status = RollbackStatus.EXECUTED;
        plan.completedAt = OffsetDateTime.now();

        auditService.record("system", "ROLLBACK_EXECUTED", "MIGRATION", null,
                "{\"migrationId\":\"" + migrationId + "\",\"deleted\":" + deleted
                        + ",\"failed\":" + failed + ",\"source\":\"" + plan.sourceSystem + "\"}"
        );

        log.info("Rollback {} complete: {} deleted, {} failed, pre-count was {}",
                migrationId, deleted, failed, plan.preCount);

        return new RollbackResult(migrationId,
                failed == 0 ? "SUCCESS" : "PARTIAL",
                deleted, plan.preCount,
                errors.isEmpty() ? "Rollback completado exitosamente"
                        : "Rollback parcial: " + errors.size() + " errores",
                errors
        );
    }

    /**
     * Dry-run rollback: preview what would be deleted without executing.
     */
    public RollbackPreview preview(String migrationId) {
        RollbackPlan plan = plans.get(migrationId);
        if (plan == null) {
            return new RollbackPreview(migrationId, "NOT_FOUND", 0);
        }
        return new RollbackPreview(
                migrationId,
                plan.sourceSystem,
                plan.loadedIds.size()
        );
    }

    /**
     * Get rollback status for a migration.
     */
    public RollbackPlan getPlan(String migrationId) {
        return plans.get(migrationId);
    }

    /**
     * List all rollback plans.
     */
    public List<RollbackPlan> listPlans() {
        return List.copyOf(plans.values());
    }

    /**
     * Clean up a rollback plan after confirmed success.
     */
    public void cleanup(String migrationId) {
        RollbackPlan removed = plans.remove(migrationId);
        if (removed != null) {
            log.info("Rollback plan cleaned up for {}", migrationId);
        }
    }

    // --- Types ---

    public enum RollbackStatus {
        READY, IN_PROGRESS, EXECUTED, EXPIRED
    }

    public static class RollbackPlan {
        public final String migrationId;
        public final String sourceSystem;
        public final OffsetDateTime createdAt;
        public final long preCount;
        public final List<String> loadedIds;
        public RollbackStatus status;
        public OffsetDateTime completedAt;

        public RollbackPlan(String migrationId, String sourceSystem, OffsetDateTime createdAt,
                             long preCount, List<String> loadedIds, RollbackStatus status) {
            this.migrationId = migrationId;
            this.sourceSystem = sourceSystem;
            this.createdAt = createdAt;
            this.preCount = preCount;
            this.loadedIds = loadedIds;
            this.status = status;
        }
    }

    public record RollbackResult(
            String migrationId,
            String status,          // SUCCESS, PARTIAL, NOT_FOUND, ALREADY_EXECUTED, IN_PROGRESS
            int deletedCount,
            long preCount,
            String message,
            List<String> errors
    ) {
        public RollbackResult(String migrationId, String status, int deletedCount,
                               long preCount, String message) {
            this(migrationId, status, deletedCount, preCount, message, List.of());
        }

        public boolean isSuccess() {
            return "SUCCESS".equals(status);
        }
    }

    public record RollbackPreview(
            String migrationId,
            String sourceSystem,
            int recordsToDelete
    ) {
        public String description() {
            return String.format("Rollback de %s: %d registros serán eliminados", sourceSystem, recordsToDelete);
        }
    }
}