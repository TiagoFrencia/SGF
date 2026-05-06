package com.sgf.integrations.anmat.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anmat")
public record AnmatProperties(
        boolean enabled,
        AnmatMode mode,
        String baseUrl,
        String reportPath,
        String establishmentGln,
        String username,
        String password,
        int timeoutMillis
) {
}
