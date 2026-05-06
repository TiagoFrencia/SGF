package com.sgf.modules.integrations.anmat.web;

import com.sgf.modules.integrations.anmat.domain.AnmatEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnmatTraceabilityEventRequest(
        @NotNull AnmatEventType eventType,
        @NotBlank String dataMatrix,
        String gln,
        UUID saleId,
        OffsetDateTime occurredAt,
        @NotBlank String source
) {
}

