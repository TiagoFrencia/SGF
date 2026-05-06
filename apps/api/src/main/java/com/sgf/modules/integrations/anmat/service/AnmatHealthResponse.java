package com.sgf.modules.integrations.anmat.service;

public record AnmatHealthResponse(
        boolean enabled,
        String mode,
        String baseUrl,
        String reportPath,
        String status
) {
}
