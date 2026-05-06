package com.sgf.integrations.afip.web;

import com.sgf.integrations.afip.service.AfipMode;
import com.sgf.integrations.afip.service.AfipWsEnvironment;
import java.time.OffsetDateTime;

public record AfipHealthResponse(
        boolean enabled,
        AfipMode mode,
        AfipWsEnvironment environment,
        String service,
        String cuit,
        Integer pointOfSale,
        String certificateStrategy,
        boolean tokenAvailable,
        OffsetDateTime tokenExpiresAt,
        String message
) {
}

