package com.sgf.integrations.anmat.web;

public record AnmatRemediationSyncResponse(
        int inconsistenciesFound,
        int remediationCasesCreated
) {
}
