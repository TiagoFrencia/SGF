package com.sgf.integrations.etl.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "etl_migration_runs")
public class EtlMigrationRun {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "migration_id", unique = true, nullable = false)
    private String migrationId;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @Column(nullable = false)
    private String status;

    @Column(name = "total_records")
    private long totalRecords;

    @Column(name = "extracted_count")
    private long extractedCount;

    @Column(name = "transformed_count")
    private long transformedCount;

    @Column(name = "passed_count")
    private long passedCount;

    @Column(name = "failed_count")
    private long failedCount;

    @Column(name = "warning_count")
    private long warningCount;

    @Column(name = "loaded_count")
    private long loadedCount;

    @Column(name = "dry_run")
    private boolean dryRun;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "last_batch_at")
    private OffsetDateTime lastBatchAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "connection_string")
    private String connectionString;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EtlMigrationFailure> failures = new ArrayList<>();

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMigrationId() { return migrationId; }
    public void setMigrationId(String migrationId) { this.migrationId = migrationId; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }
    public long getExtractedCount() { return extractedCount; }
    public void setExtractedCount(long extractedCount) { this.extractedCount = extractedCount; }
    public long getTransformedCount() { return transformedCount; }
    public void setTransformedCount(long transformedCount) { this.transformedCount = transformedCount; }
    public long getPassedCount() { return passedCount; }
    public void setPassedCount(long passedCount) { this.passedCount = passedCount; }
    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
    public long getWarningCount() { return warningCount; }
    public void setWarningCount(long warningCount) { this.warningCount = warningCount; }
    public long getLoadedCount() { return loadedCount; }
    public void setLoadedCount(long loadedCount) { this.loadedCount = loadedCount; }
    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getLastBatchAt() { return lastBatchAt; }
    public void setLastBatchAt(OffsetDateTime lastBatchAt) { this.lastBatchAt = lastBatchAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public String getConnectionString() { return connectionString; }
    public void setConnectionString(String connectionString) { this.connectionString = connectionString; }
    public List<EtlMigrationFailure> getFailures() { return failures; }
    public void setFailures(List<EtlMigrationFailure> failures) { this.failures = failures; }
}
