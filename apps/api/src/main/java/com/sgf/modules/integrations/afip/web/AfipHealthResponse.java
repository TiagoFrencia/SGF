package com.sgf.modules.integrations.afip.web;

import com.sgf.modules.integrations.afip.service.AfipMode;
import com.sgf.modules.integrations.afip.service.AfipWsEnvironment;
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

