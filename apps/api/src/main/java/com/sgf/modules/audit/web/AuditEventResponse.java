package com.sgf.modules.audit.web;

import com.sgf.modules.audit.domain.AuditEvent;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String actorUsername,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String detailsJson,
        OffsetDateTime createdAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActorUsername(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getDetailsJson(),
                event.getCreatedAt()
        );
    }
}

