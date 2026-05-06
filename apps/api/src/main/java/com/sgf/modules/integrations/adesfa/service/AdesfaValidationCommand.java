package com.sgf.modules.integrations.adesfa.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdesfaValidationCommand(
        UUID saleId,
        String validatorCode,
        String actionCode,
        String affiliateNumber,
        String prescriptionNumber,
        BigDecimal totalAmount,
        OffsetDateTime requestedAt
) {
}
