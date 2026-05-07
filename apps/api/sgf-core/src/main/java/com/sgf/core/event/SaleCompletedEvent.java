package com.sgf.core.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published when a sale is completed.
 */
public record SaleCompletedEvent(
    UUID saleId,
    String idempotencyKey,
    java.math.BigDecimal totalAmount,
    String actorUsername,
    OffsetDateTime occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() { return saleId; }

    @Override
    public String aggregateType() { return "SALE"; }

    @Override
    public String eventType() { return "SALE_COMPLETED"; }

    @Override
    public String payload() {
        return "{\"idempotencyKey\":\"" + idempotencyKey + "\",\"total\":" + totalAmount + "}";
    }

    @Override
    public OffsetDateTime occurredAt() { return occurredAt; }
}
