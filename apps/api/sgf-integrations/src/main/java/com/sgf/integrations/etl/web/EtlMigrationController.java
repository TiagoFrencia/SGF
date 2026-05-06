package com.sgf.integrations.etl.web;

import com.sgf.integrations.etl.MigrationDashboard;
import com.sgf.integrations.etl.MigrationDashboard.BatchProgress;
import com.sgf.integrations.etl.MigrationDashboard.DashboardSnapshot;
import com.sgf.integrations.etl.MigrationDashboard.MigrationStatus;
import com.sgf.integrations.etl.validate.DataValidator.FailedRecord;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the ETL migration dashboard.
 *
 * Endpoints:
 * - POST /api/etl/migrations — Start a new migration
 * - GET /api/etl/migrations — List all migrations
 * - GET /api/etl/migrations/{id} — Dashboard snapshot
 * - GET /api/etl/migrations/{id}/failed — Failed records for review
 * - POST /api/etl/migrations/{id}/batch — Execute next batch
 * - POST /api/etl/migrations/{id}/execute — Run to completion
 * - POST /api/etl/migrations/{id}/pause — Pause
 * - POST /api/etl/migrations/{id}/resume — Resume
 * - POST /api/etl/migrations/{id}/abort — Abort
 * - POST /api/etl/migrations/{id}/retry-failed — Re-process failed records
 */
@RestController
@RequestMapping("/api/etl/migrations")
public class EtlMigrationController {

    private static final Logger log = LoggerFactory.getLogger(EtlMigrationController.class);

    private final MigrationDashboard dashboard;

    public EtlMigrationController(MigrationDashboard dashboard) {
        this.dashboard = dashboard;
    }

    /**
     * Start a new ETL migration.
     *
     * Request body:
     * {
     *   "sourceSystem": "FarmaWin" | "Nixfarma" | "DBF_Generic",
     *   "connectionString": "path/to/data" (optional for test/extractor default),
     *   "dryRun": boolean (default false)
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> startMigration(@RequestBody StartMigrationRequest request) {
        String source = request.sourceSystem();
        String connection = request.connectionString() != null ? request.connectionString() : source;
        boolean dryRun = request.dryRun() != null && request.dryRun();

        String migrationId = dashboard.startMigration(source, connection, dryRun);
        return ResponseEntity.ok(Map.of(
                "migrationId", migrationId,
                "status", "STARTED",
                "message", "Migration " + migrationId + " iniciada desde " + source
                        + (dryRun ? " (modo dry-run)" : "")
        ));
    }

    /**
     * List all migrations (active and completed).
     */
    @GetMapping
    public ResponseEntity<List<DashboardSnapshot>> listMigrations() {
        return ResponseEntity.ok(dashboard.listMigrations());
    }

    /**
     * Get the full dashboard snapshot for a migration.
     */
    @GetMapping("/{migrationId}")
    public ResponseEntity<DashboardSnapshot> getDashboard(@PathVariable String migrationId) {
        DashboardSnapshot snapshot = dashboard.getDashboard(migrationId);
        if (snapshot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Get failed records for review and manual repair.
     */
    @GetMapping("/{migrationId}/failed")
    public ResponseEntity<List<FailedRecord>> getFailedRecords(@PathVariable String migrationId) {
        DashboardSnapshot snapshot = dashboard.getDashboard(migrationId);
        if (snapshot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot.failedRecords());
    }

    /**
     * Execute the next batch of records.
     */
    @PostMapping("/{migrationId}/batch")
    public ResponseEntity<BatchProgress> executeBatch(@PathVariable String migrationId,
                                                       @RequestParam(defaultValue = "100") int batchSize) {
        try {
            BatchProgress progress = dashboard.executeBatch(migrationId, batchSize);
            return ResponseEntity.ok(progress);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Run the migration to completion.
     */
    @PostMapping("/{migrationId}/execute")
    public ResponseEntity<Map<String, String>> executeFull(@PathVariable String migrationId) {
        try {
            MigrationStatus status = dashboard.executeFull(migrationId);
            return ResponseEntity.ok(Map.of(
                    "migrationId", migrationId,
                    "status", status.name(),
                    "message", "Migración completada con estado: " + status
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Pause a running migration.
     */
    @PostMapping("/{migrationId}/pause")
    public ResponseEntity<Map<String, String>> pause(@PathVariable String migrationId) {
        dashboard.pause(migrationId);
        return ResponseEntity.ok(Map.of(
                "migrationId", migrationId,
                "action", "PAUSED"
        ));
    }

    /**
     * Resume a paused migration.
     */
    @PostMapping("/{migrationId}/resume")
    public ResponseEntity<Map<String, String>> resume(@PathVariable String migrationId) {
        dashboard.resume(migrationId);
        return ResponseEntity.ok(Map.of(
                "migrationId", migrationId,
                "action", "RESUMED"
        ));
    }

    /**
     * Abort and clean up a migration.
     */
    @PostMapping("/{migrationId}/abort")
    public ResponseEntity<Map<String, String>> abort(@PathVariable String migrationId) {
        dashboard.abort(migrationId);
        return ResponseEntity.ok(Map.of(
                "migrationId", migrationId,
                "action", "ABORTED"
        ));
    }

    // --- DTOs ---

    public record StartMigrationRequest(
            String sourceSystem,
            String connectionString,
            Boolean dryRun
    ) {}
}