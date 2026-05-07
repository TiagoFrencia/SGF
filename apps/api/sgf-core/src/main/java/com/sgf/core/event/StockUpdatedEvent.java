package com.sgf.core.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published when stock levels are modified.
 */
public record StockUpdatedEvent(
    UUID productId,
    int quantityChange,
    int currentStock,
    String reason,
    String actorUsername,
    OffsetDateTime occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() { return productId; }

    @Override
    public String aggregateType() { return "PRODUCT"; }

    @Override
    public String eventType() { return "STOCK_UPDATED"; }

    @Override
    public String payload() {
        return "{\"change\":" + quantityChange + ",\"current\":" + currentStock + ",\"reason\":\"" + reason + "\"}";
    }

    @Override
    public OffsetDateTime occurredAt() { return occurredAt; }
}
