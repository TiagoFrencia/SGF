package com.sgf.integrations.anmat.web;

import java.util.List;

public record AnmatTraceabilityInconsistencyResponse(
        String gtin,
        String serialNumber,
        String lotNumber,
        String severity,
        String issueCode,
        String message,
        String recommendation,
        List<String> eventTypes
) {
}
