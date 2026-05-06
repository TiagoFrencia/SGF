package com.sgf.modules.integrations.afip.wsfe;

import com.sgf.modules.integrations.afip.domain.AfipInvoiceStatus;
import java.time.LocalDate;
import java.util.List;

public record WsfeAuthorizeResponse(
        AfipInvoiceStatus status,
        String resultCode,
        String cae,
        LocalDate caeDueDate,
        String rawXml,
        List<com.sgf.modules.integrations.afip.service.AfipMessage> observations,
        List<com.sgf.modules.integrations.afip.service.AfipMessage> errors
) {
}
