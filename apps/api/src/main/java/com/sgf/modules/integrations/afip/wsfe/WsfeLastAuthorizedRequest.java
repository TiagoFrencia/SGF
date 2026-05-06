package com.sgf.modules.integrations.afip.wsfe;

import com.sgf.modules.integrations.afip.domain.AfipInvoiceType;

public record WsfeLastAuthorizedRequest(
        String token,
        String sign,
        String cuit,
        int pointOfSale,
        AfipInvoiceType invoiceType
) {
}

