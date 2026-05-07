package com.sgf.core.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published when a migration job finishes.
 */
public record MigrationFinishedEvent(
    String migrationId,
    String status,
    long passedCount,
    long failedCount,
    OffsetDateTime occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() { return UUID.nameUUIDFromBytes(migrationId.getBytes()); }

    @Override
    public String aggregateType() { return "MIGRATION"; }

    @Override
    public String eventType() { return "MIGRATION_FINISHED"; }

    @Override
    public String payload() {
        return "{\"status\":\"" + status + "\",\"passed\":" + passedCount + ",\"failed\":" + failedCount + "}";
    }

    @Override
    public OffsetDateTime occurredAt() { return occurredAt; }
}
