package com.sgf.integrations.etl.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "etl_migration_failures")
public class EtlMigrationFailure {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private EtlMigrationRun run;

    private String gtin;
    private String sku;

    @Column(name = "commercial_name")
    private String commercialName;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    private String rawData;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public EtlMigrationRun getRun() { return run; }
    public void setRun(EtlMigrationRun run) { this.run = run; }
    public String getGtin() { return gtin; }
    public void setGtin(String gtin) { this.gtin = gtin; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getCommercialName() { return commercialName; }
    public void setCommercialName(String commercialName) { this.commercialName = commercialName; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
}
