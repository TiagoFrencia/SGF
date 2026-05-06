package com.sgf.integrations.afip.wsfe;

import com.sgf.integrations.afip.domain.AfipDocumentType;
import com.sgf.integrations.afip.domain.AfipInvoiceType;
import java.math.BigDecimal;

public record WsfeAuthorizeRequest(
        String token,
        String sign,
        String cuit,
        int pointOfSale,
        AfipInvoiceType invoiceType,
        AfipDocumentType documentType,
        String documentNumber,
        BigDecimal totalAmount,
        String currencyCode,
        long voucherNumberFrom,
        long voucherNumberTo
) {
}
