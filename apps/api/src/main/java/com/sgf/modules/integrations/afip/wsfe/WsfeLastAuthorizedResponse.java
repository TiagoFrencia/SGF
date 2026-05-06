package com.sgf.modules.integrations.afip.wsfe;

public record WsfeLastAuthorizedResponse(
        long lastAuthorizedNumber,
        String rawXml
) {
}

