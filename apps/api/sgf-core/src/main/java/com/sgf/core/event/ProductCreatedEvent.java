package com.sgf.core.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published when a new product is added to the catalog.
 */
public record ProductCreatedEvent(
    UUID productId,
    String gtin,
    String commercialName,
    String actorUsername,
    String tenantId,
    OffsetDateTime occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() { return productId; }

    @Override
    public String tenantId() { return tenantId; }

    @Override
    public String aggregateType() { return "PRODUCT"; }

    @Override
    public String eventType() { return "PRODUCT_CREATED"; }

    @Override
    public String payload() {
        return "{\"gtin\":\"" + gtin + "\",\"name\":\"" + commercialName + "\"}";
    }

    @Override
    public OffsetDateTime occurredAt() { return occurredAt; }
}
