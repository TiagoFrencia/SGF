package com.sgf.integrations.anmat.web;

import com.sgf.integrations.anmat.domain.AnmatTraceabilityEvent;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnmatTraceabilityEventResponse(
        UUID id,
        UUID productId,
        UUID batchId,
        UUID saleId,
        String eventType,
        String eventStatus,
        String gtin,
        String serialNumber,
        String lotNumber,
        LocalDate expiresAt,
        String gln,
        OffsetDateTime occurredAt,
        String source,
        String requestJson,
        String responseJson,
        String errorMessage,
        String providerReference,
        String integrationMode,
        Integer lastHttpStatus,
        boolean retryable
) {
    public static AnmatTraceabilityEventResponse from(AnmatTraceabilityEvent event) {
        return new AnmatTraceabilityEventResponse(
                event.getId(),
                event.getProduct().getId(),
                event.getBatch() != null ? event.getBatch().getId() : null,
                event.getSale() != null ? event.getSale().getId() : null,
                event.getEventType().name(),
                event.getEventStatus().name(),
                event.getGtin(),
                event.getSerialNumber(),
                event.getLotNumber(),
                event.getExpiresAt(),
                event.getGln(),
                event.getOccurredAt(),
                event.getSource(),
                event.getRequestJson(),
                event.getResponseJson(),
                event.getErrorMessage(),
                event.getProviderReference(),
                event.getIntegrationMode(),
                event.getLastHttpStatus(),
                event.isRetryable()
        );
    }
}
