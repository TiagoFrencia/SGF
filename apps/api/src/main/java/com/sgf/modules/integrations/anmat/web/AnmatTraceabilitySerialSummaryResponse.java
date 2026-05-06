package com.sgf.modules.integrations.anmat.web;

import java.util.List;

public record AnmatTraceabilitySerialSummaryResponse(
        String gtin,
        String serialNumber,
        String lotNumber,
        String currentState,
        boolean hasReceipt,
        boolean hasDispense,
        boolean hasReturn,
        int totalEvents,
        List<AnmatTraceabilityEventResponse> timeline
) {
}
