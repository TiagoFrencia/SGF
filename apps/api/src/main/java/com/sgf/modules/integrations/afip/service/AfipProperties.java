package com.sgf.modules.integrations.afip.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.afip")
public record AfipProperties(
        boolean enabled,
        AfipMode mode,
        AfipWsEnvironment wsEnvironment,
        String cuit,
        int pointOfSale,
        String certificatePath,
        String privateKeyPath,
        String pkcs12Path,
        String pkcs12Password,
        String pkcs12Alias,
        String service
) {
}
