package com.sgf.core.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event contract for CQRS/Event Sourcing.
 * Used by the offline sync engine and outbox pattern.
 */
public interface DomainEvent {

    UUID aggregateId();

    String aggregateType();

    String eventType();

    String payload();

    OffsetDateTime occurredAt();
}