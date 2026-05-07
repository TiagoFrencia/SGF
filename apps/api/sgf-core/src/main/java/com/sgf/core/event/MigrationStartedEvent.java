package com.sgf.core.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published when a migration job starts.
 */
public record MigrationStartedEvent(
    String migrationId,
    String sourceSystem,
    long totalRecords,
    boolean dryRun,
    OffsetDateTime occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() { return UUID.nameUUIDFromBytes(migrationId.getBytes()); }

    @Override
    public String aggregateType() { return "MIGRATION"; }

    @Override
    public String eventType() { return "MIGRATION_STARTED"; }

    @Override
    public String payload() {
        return "{\"source\":\"" + sourceSystem + "\",\"total\":" + totalRecords + ",\"dryRun\":" + dryRun + "}";
    }

    @Override
    public OffsetDateTime occurredAt() { return occurredAt; }
}
