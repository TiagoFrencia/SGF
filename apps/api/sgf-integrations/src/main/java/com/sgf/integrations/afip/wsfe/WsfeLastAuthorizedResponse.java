package com.sgf.integrations.afip.wsfe;

public record WsfeLastAuthorizedResponse(
        long lastAuthorizedNumber,
        String rawXml
) {
}

