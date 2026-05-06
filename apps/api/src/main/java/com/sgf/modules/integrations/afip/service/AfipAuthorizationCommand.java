package com.sgf.modules.integrations.afip.service;

import com.sgf.modules.integrations.afip.domain.AfipDocumentType;
import com.sgf.modules.integrations.afip.domain.AfipInvoiceType;
import java.math.BigDecimal;
import java.util.UUID;

public record AfipAuthorizationCommand(
        UUID saleId,
        String cuit,
        int pointOfSale,
        AfipInvoiceType invoiceType,
        AfipDocumentType customerDocumentType,
        String customerDocumentNumber,
        BigDecimal totalAmount,
        String currencyCode,
        long voucherNumberFrom,
        long voucherNumberTo
) {
}
