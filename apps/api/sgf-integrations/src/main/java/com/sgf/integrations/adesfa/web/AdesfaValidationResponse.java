package com.sgf.integrations.adesfa.web;

import com.sgf.integrations.adesfa.domain.AdesfaValidation;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdesfaValidationResponse(
        UUID id,
        UUID saleId,
        String validatorCode,
        String actionCode,
        String affiliateNumber,
        String prescriptionNumber,
        String status,
        BigDecimal totalAmount,
        BigDecimal patientAmount,
        BigDecimal coverageAmount,
        OffsetDateTime validatedAt,
        String requestJson,
        String responseJson,
        String errorMessage,
        String providerReference,
        String integrationMode,
        Integer lastHttpStatus,
        boolean retryable
) {
    public static AdesfaValidationResponse from(AdesfaValidation validation) {
        return new AdesfaValidationResponse(
                validation.getId(),
                validation.getSale().getId(),
                validation.getValidatorCode(),
                validation.getActionCode(),
                validation.getAffiliateNumber(),
                validation.getPrescriptionNumber(),
                validation.getStatus().name(),
                validation.getTotalAmount(),
                validation.getPatientAmount(),
                validation.getCoverageAmount(),
                validation.getValidatedAt(),
                validation.getRequestJson(),
                validation.getResponseJson(),
                validation.getErrorMessage(),
                validation.getProviderReference(),
                validation.getIntegrationMode(),
                validation.getLastHttpStatus(),
                validation.isRetryable()
        );
    }
}
