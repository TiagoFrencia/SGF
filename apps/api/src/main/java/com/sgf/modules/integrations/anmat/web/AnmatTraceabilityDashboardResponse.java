package com.sgf.modules.integrations.anmat.web;

import java.util.List;

public record AnmatTraceabilityDashboardResponse(
        long totalEvents,
        long receipts,
        long dispenses,
        long returns,
        long failedEvents,
        long serialsWithCurrentDispense,
        long inconsistentSerials,
        List<AnmatTraceabilityInconsistencyResponse> recentInconsistencies
) {
}
