package com.sgf.modules.integrations.afip.web;

import com.sgf.modules.integrations.afip.domain.AfipDocumentType;
import com.sgf.modules.integrations.afip.domain.AfipInvoiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AfipAuthorizeInvoiceRequest(
        @NotNull AfipInvoiceType invoiceType,
        @NotNull AfipDocumentType customerDocumentType,
        @NotBlank String customerDocumentNumber,
        Integer pointOfSale,
        @NotBlank String currencyCode
) {
}

