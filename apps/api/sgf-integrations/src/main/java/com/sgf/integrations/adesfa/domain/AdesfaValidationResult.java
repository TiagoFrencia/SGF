package com.sgf.integrations.adesfa.domain;

import java.math.BigDecimal;

public record AdesfaValidationResult(
        boolean success,
        String status,
        String message,
        BigDecimal coverageAmount,
        BigDecimal patientAmount,
        String reference
) {
}
