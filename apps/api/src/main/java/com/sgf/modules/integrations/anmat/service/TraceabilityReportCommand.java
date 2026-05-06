package com.sgf.modules.integrations.anmat.service;

import com.sgf.modules.integrations.anmat.domain.AnmatEventType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TraceabilityReportCommand(
        UUID productId,
        UUID batchId,
        UUID saleId,
        AnmatEventType eventType,
        String gtin,
        String serialNumber,
        String lotNumber,
        java.time.LocalDate expiresAt,
        String gln,
        OffsetDateTime occurredAt
) {
}

