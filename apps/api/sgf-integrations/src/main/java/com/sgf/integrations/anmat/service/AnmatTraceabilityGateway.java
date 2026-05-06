package com.sgf.integrations.anmat.service;

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
