package com.sgf.integrations.adesfa.web;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdesfaValidationRequest(
        String validatorCode,
        @NotBlank String actionCode,
        @NotBlank String affiliateNumber,
        @NotBlank String prescriptionNumber,
        List<ValidationItemRequest> items
) {
    public record ValidationItemRequest(
            UUID productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}
