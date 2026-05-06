package com.sgf.modules.integrations.anmat.web;

public record AnmatRemediationSyncResponse(
        int inconsistenciesFound,
        int remediationCasesCreated
) {
}
