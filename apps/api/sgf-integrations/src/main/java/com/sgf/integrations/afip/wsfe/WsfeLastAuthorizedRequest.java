package com.sgf.integrations.afip.wsfe;

import com.sgf.integrations.afip.domain.AfipInvoiceType;

public record WsfeLastAuthorizedRequest(
        String token,
        String sign,
        String cuit,
        int pointOfSale,
        AfipInvoiceType invoiceType
) {
}

