package com.sgf.integrations.afip.service;

import com.sgf.integrations.afip.domain.AfipInvoiceStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AfipAuthorizationResult(
        AfipInvoiceStatus status,
        long voucherNumberFrom,
        long voucherNumberTo,
        String afipResultCode,
        String cae,
        LocalDate caeDueDate,
        String providerReference,
        String responseJson,
        List<AfipMessage> observations,
        List<AfipMessage> errors,
        OffsetDateTime tokenExpiresAt
) {
}
