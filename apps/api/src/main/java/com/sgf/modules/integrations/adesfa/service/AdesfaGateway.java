package com.sgf.modules.integrations.adesfa.service;

import java.math.BigDecimal;

public interface AdesfaGateway {
    GatewayResult validate(AdesfaValidationCommand command);

    record GatewayResult(
            boolean success,
            String payload,
            String errorMessage,
            String providerReference,
            Integer httpStatus,
            boolean retryable,
            String integrationMode,
            BigDecimal patientAmount,
            BigDecimal coverageAmount
    ) {
    }
}
