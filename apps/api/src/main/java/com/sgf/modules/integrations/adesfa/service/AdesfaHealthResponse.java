package com.sgf.modules.integrations.adesfa.service;

public record AdesfaHealthResponse(
        boolean enabled,
        String mode,
        String baseUrl,
        String validationPath,
        String status
) {
}
