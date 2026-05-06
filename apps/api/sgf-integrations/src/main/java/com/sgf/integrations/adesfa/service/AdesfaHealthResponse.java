package com.sgf.integrations.adesfa.service;

public record AdesfaHealthResponse(
        boolean enabled,
        String mode,
        String baseUrl,
        String validationPath,
        String status
) {
}
