package com.sgf.integrations.adesfa.web;

import jakarta.validation.constraints.NotBlank;

public record AdesfaValidationRequest(
        String validatorCode,
        @NotBlank String actionCode,
        @NotBlank String affiliateNumber,
        @NotBlank String prescriptionNumber
) {
}
