package com.sgf.modules.integrations.afip.wsaa;

import java.time.OffsetDateTime;

public record WsaaLoginResponse(
        String token,
        String sign,
        OffsetDateTime expirationTime,
        String rawXml
) {
}

