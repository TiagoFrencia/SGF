package com.sgf.modules.integrations.afip.web;

import com.sgf.modules.integrations.afip.domain.AfipInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AfipInvoiceResponse(
        UUID id,
        UUID saleId,
        Integer pointOfSale,
        String invoiceType,
        String customerDocumentType,
        String customerDocumentNumber,
        String currencyCode,
        BigDecimal netAmount,
        BigDecimal totalAmount,
        String status,
        Long voucherNumberFrom,
        Long voucherNumberTo,
        String afipResultCode,
        String cae,
        LocalDate caeDueDate,
        String providerReference,
        OffsetDateTime authorizedAt,
        Integer retryCount,
        OffsetDateTime lastAttemptedAt,
        OffsetDateTime tokenExpiresAt,
        String observationsJson,
        String errorsJson,
        String lastErrorCode,
        String lastErrorMessage,
        String requestJson,
        String responseJson
) {
    public static AfipInvoiceResponse from(AfipInvoice invoice) {
        return new AfipInvoiceResponse(
                invoice.getId(),
                invoice.getSale().getId(),
                invoice.getPointOfSale(),
                invoice.getInvoiceType().name(),
                invoice.getCustomerDocumentType().name(),
                invoice.getCustomerDocumentNumber(),
                invoice.getCurrencyCode(),
                invoice.getNetAmount(),
                invoice.getTotalAmount(),
                invoice.getStatus().name(),
                invoice.getVoucherNumberFrom(),
                invoice.getVoucherNumberTo(),
                invoice.getAfipResultCode(),
                invoice.getCae(),
                invoice.getCaeDueDate(),
                invoice.getProviderReference(),
                invoice.getAuthorizedAt(),
                invoice.getRetryCount(),
                invoice.getLastAttemptedAt(),
                invoice.getTokenExpiresAt(),
                invoice.getObservationsJson(),
                invoice.getErrorsJson(),
                invoice.getLastErrorCode(),
                invoice.getLastErrorMessage(),
                invoice.getRequestJson(),
                invoice.getResponseJson()
        );
    }
}
