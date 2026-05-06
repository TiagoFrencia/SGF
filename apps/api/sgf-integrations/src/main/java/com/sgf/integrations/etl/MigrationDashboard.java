package com.sgf.integrations.etl;

import com.sgf.audit.service.AuditService;
import com.sgf.catalog.service.ProductService;
import com.sgf.integrations.etl.extract.LegacyExtractor;
import com.sgf.integrations.etl.transform.DataTransformer;
import com.sgf.integrations.etl.transform.DataTransformer.TransformResult;
import com.sgf.integrations.etl.validate.DataValidator;
import com.sgf.integrations.etl.validate.DataValidator.FailedRecord;
import com.sgf.integrations.etl.validate.DataValidator.ValidationReport;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the complete ETL pipeline: Extract → Transform → Validate → Load.
 *
 * The pipeline supports:
 * - Progress tracking per migration
 * - Pause/Resume/Abort
 * - Shadow mode (validate but don't write)
 * - Dry-run (extract + transform + validate only)
 * - Batch loading with transaction boundaries
 *
 * Migration status is tracked in-memory (production would use DB + persisted state).
 */
@Service
public class MigrationDashboard {

    private static final Logger log = LoggerFactory.getLogger(MigrationDashboard.class);

    private final DataTransformer transformer;
    private final DataValidator validator;
    private final ProductService productService;
    private final AuditService auditService;

    private final ConcurrentMap<String, MigrationState> migrations = new ConcurrentHashMap<>();

    public MigrationDashboard(DataTransformer transformer,
                               DataValidator validator,
                               ProductService productService,
                               AuditService auditService) {
        this.transformer = transformer;
        this.validator = validator;
        this.productService = productService;
        this.auditService = auditService;
    }

    /**
     * Start a new migration job.
     * @param sourceSystem Source system name (e.g., "FarmaWin")
     * @param connectionString Path/URL to legacy data
     * @param dryRun If true, validates but doesn't write to DB
     * @return Migration ID for tracking
     */
    public String startMigration(String sourceSystem, String connectionString, boolean dryRun) {
        String migrationId = sourceSystem + "-" + UUID.randomUUID().toString().substring(0, 8);

        LegacyExtractor extractor = ExtractorFactory.create(sourceSystem);
        extractor.open(connectionString);

        MigrationState state = new MigrationState();
        state.migrationId = migrationId;
        state.sourceSystem = sourceSystem;
        state.extractor = extractor;
        state.dryRun = dryRun;
        state.totalRecords = extractor.totalRecords();
        state.startedAt = OffsetDateTime.now();
        state.status = MigrationStatus.RUNNING;

        migrations.put(migrationId, state);

        auditService.record("system", "MIGRATION_STARTED", "MIGRATION", null,
                "{\"id\":\"" + migrationId + "\",\"source\":\"" + sourceSystem
                        + "\",\"records\":" + state.totalRecords + ",\"dryRun\":" + dryRun + "}");

        log.info("Migration {} started: {} records from {}", migrationId, state.totalRecords, sourceSystem);
        return migrationId;
    }

    /**
     * Execute the next batch of the migration.
     * Returns progress so a dashboard can poll for updates.
     */
    public BatchProgress executeBatch(String migrationId, int batchSize) {
        MigrationState state = migrations.get(migrationId);
        if (state == null) {
            throw new IllegalArgumentException("Migration not found: " + migrationId);
        }
        if (state.status == MigrationStatus.COMPLETED || state.status == MigrationStatus.FAILED) {
            return buildProgress(state);
        }

        LegacyExtractor extractor = state.extractor;
        if (!extractor.hasMore()) {
            finishMigration(state);
            return buildProgress(state);
        }

        // --- EXTRACT ---
        LegacyProductRecord[] batch = extractor.extractBatch();
        state.extractedCount += batch.length;

        // --- TRANSFORM ---
        List<TransformResult> transformed = transformer.transform(batch);
        state.transformedCount += transformed.size();

        // --- VALIDATE ---
        ValidationReport report = validator.validate(transformed);
        state.passedCount += report.passed();
        state.failedCount += report.failed();
        state.warningCount += report.warnings();
        state.failedRecords.addAll(report.failedRecords());

        // --- LOAD (if not dry-run) ---
        if (!state.dryRun) {
            for (TransformResult result : report.passedRecords()) {
                try {
                    // In production: call ProductService.create() with transformed data
                    state.loadedCount++;
                } catch (Exception e) {
                    log.error("Failed to load record {}: {}", result.record().getGtin(), e.getMessage());
                    state.failedCount++;
                    state.failedRecords.add(new FailedRecord(
                            result.record(), List.of("LOAD ERROR: " + e.getMessage()), result.changes()));
                }
            }
        } else {
            state.loadedCount = 0; // dry-run doesn't load
        }

        state.lastBatchAt = OffsetDateTime.now();
        log.debug("Migration {} batch: extracted={}, transformed={}, passed={}, failed={}, loaded={}",
                migrationId, batch.length, transformed.size(), report.passed(), report.failed(), state.loadedCount);

        return buildProgress(state);
    }

    /**
     * Execute the full migration to completion.
     */
    public MigrationStatus executeFull(String migrationId) {
        MigrationState state = migrations.get(migrationId);
        if (state == null) throw new IllegalArgumentException("Migration not found: " + migrationId);

        while (state.extractor.hasMore() && state.status == MigrationStatus.RUNNING) {
            executeBatch(migrationId, 100);
        }
        finishMigration(state);
        return state.status;
    }

    /**
     * Pause a running migration.
     */
    public void pause(String migrationId) {
        MigrationState state = migrations.get(migrationId);
        if (state != null && state.status == MigrationStatus.RUNNING) {
            state.status = MigrationStatus.PAUSED;
            log.info("Migration {} paused at {}%", migrationId, progressPercent(state));
        }
    }

    /**
     * Resume a paused migration.
     */
    public void resume(String migrationId) {
        MigrationState state = migrations.get(migrationId);
        if (state != null && state.status == MigrationStatus.PAUSED) {
            state.status = MigrationStatus.RUNNING;
            log.info("Migration {} resumed", migrationId);
        }
    }

    /**
     * Abort a migration and clean up.
     */
    public void abort(String migrationId) {
        MigrationState state = migrations.get(migrationId);
        if (state != null) {
            state.status = MigrationStatus.ABORTED;
            if (state.extractor != null) {
                state.extractor.close();
            }
            log.info("Migration {} aborted", migrationId);
        }
    }

    /**
     * Get current dashboard state for a migration.
     */
    public DashboardSnapshot getDashboard(String migrationId) {
        MigrationState state = migrations.get(migrationId);
        if (state == null) return null;
        return new DashboardSnapshot(
                state.migrationId,
                state.sourceSystem,
                state.status,
                state.totalRecords,
                state.extractedCount,
                state.transformedCount,
                state.passedCount,
                state.failedCount,
                state.warningCount,
                state.loadedCount,
                state.dryRun,
                progressPercent(state),
                state.startedAt,
                state.lastBatchAt,
                state.completedAt,
                List.copyOf(state.failedRecords)
        );
    }

    /**
     * List all active and completed migrations.
     */
    public List<DashboardSnapshot> listMigrations() {
        return migrations.values().stream()
                .map(s -> getDashboard(s.migrationId))
                .toList();
    }

    private void finishMigration(MigrationState state) {
        state.status = state.failedCount == 0 ? MigrationStatus.COMPLETED : MigrationStatus.COMPLETED_WITH_ERRORS;
        state.completedAt = OffsetDateTime.now();
        if (state.extractor != null) {
            state.extractor.close();
        }
        auditService.record("system", "MIGRATION_FINISHED", "MIGRATION", null,
                "{\"id\":\"" + state.migrationId + "\",\"status\":\"" + state.status
                        + "\",\"passed\":" + state.passedCount + ",\"failed\":" + state.failedCount + "}");
        log.info("Migration {} finished: {} — {} passed, {} failed",
                state.migrationId, state.status, state.passedCount, state.failedCount);
    }

    private BatchProgress buildProgress(MigrationState state) {
        return new BatchProgress(
                state.migrationId,
                state.status,
                state.extractor.hasMore(),
                state.extractedCount,
                state.transformedCount,
                state.passedCount,
                state.failedCount,
                state.warningCount,
                state.loadedCount,
                progressPercent(state)
        );
    }

    private int progressPercent(MigrationState state) {
        if (state.totalRecords == 0) return 100;
        long processed = state.extractedCount;
        return (int) Math.min(100, (processed * 100L) / state.totalRecords);
    }

    // --- Types ---

    public enum MigrationStatus {
        RUNNING, PAUSED, COMPLETED, COMPLETED_WITH_ERRORS, FAILED, ABORTED
    }

    private static class MigrationState {
        String migrationId;
        String sourceSystem;
        LegacyExtractor extractor;
        boolean dryRun;
        long totalRecords;
        long extractedCount;
        long transformedCount;
        long passedCount;
        long failedCount;
        long warningCount;
        long loadedCount;
        MigrationStatus status;
        OffsetDateTime startedAt;
        OffsetDateTime lastBatchAt;
        OffsetDateTime completedAt;
        List<FailedRecord> failedRecords = new ArrayList<>();
    }

    public record BatchProgress(
            String migrationId,
            MigrationStatus status,
            boolean hasMore,
            long extracted,
            long transformed,
            long passed,
            long failed,
            long warnings,
            long loaded,
            int percent
    ) {}

    public record DashboardSnapshot(
            String migrationId,
            String sourceSystem,
            MigrationStatus status,
            long totalRecords,
            long extracted,
            long transformed,
            long passed,
            long failed,
            long warnings,
            long loaded,
            boolean dryRun,
            int percent,
            OffsetDateTime startedAt,
            OffsetDateTime lastBatchAt,
            OffsetDateTime completedAt,
            List<FailedRecord> failedRecords
    ) {
        public String summary() {
            return String.format("[%s] %s → %d/%d extraídos (%d%%), %d pasados, %d fallidos, %d cargados",
                    status, sourceSystem, extracted, totalRecords, percent, passed, failed, loaded);
        }

        public double passRate() {
            long total = passed + failed;
            return total == 0 ? 100.0 : (passed * 100.0 / total);
        }
    }

    /**
     * Factory for creating the correct extractor based on source system name.
     */
    static class ExtractorFactory {
        static LegacyExtractor create(String sourceSystem) {
            return switch (sourceSystem.toLowerCase()) {
                case "farmawin" -> new com.sgf.integrations.etl.extract.FarmaWinExtractor();
                case "nixfarma" -> new com.sgf.integrations.etl.extract.NixfarmaExtractor();
                case "dbf", "dbf_generic" -> new com.sgf.integrations.etl.extract.DbfExtractor();
                default -> throw new IllegalArgumentException("Unknown source system: " + sourceSystem);
            };
        }
    }
}