package com.sgf.modules.integrations.anmat.service;

public interface AnmatTraceabilityGateway {
    GatewayResult report(TraceabilityReportCommand command);

    record GatewayResult(
            boolean success,
            String payload,
            String errorMessage,
            String providerReference,
            Integer httpStatus,
            boolean retryable,
            String integrationMode
    ) {
    }
}
