package com.sgf.modules.integrations.adesfa.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.adesfa")
public record AdesfaProperties(
        boolean enabled,
        AdesfaMode mode,
        String baseUrl,
        String validationPath,
        String softwareCode,
        String providerCode,
        String defaultValidatorCode,
        String username,
        String password,
        int timeoutMillis
) {
}
