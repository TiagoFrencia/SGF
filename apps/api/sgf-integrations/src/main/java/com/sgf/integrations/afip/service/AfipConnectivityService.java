package com.sgf.integrations.afip.service;

import com.sgf.core.domain.BadRequestException;
import com.sgf.integrations.afip.web.AfipHealthResponse;
import com.sgf.integrations.afip.wsaa.AfipTokenService;
import com.sgf.integrations.afip.wsaa.WsaaLoginResponse;
import org.springframework.stereotype.Service;

@Service
public class AfipConnectivityService {

    private final AfipProperties properties;
    private final AfipTokenService tokenService;

    public AfipConnectivityService(AfipProperties properties, AfipTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    public AfipHealthResponse inspect(boolean forceTokenRefresh) {
        if (!properties.enabled()) {
            return new AfipHealthResponse(false, properties.mode(), properties.wsEnvironment(), properties.service(),
                    properties.cuit(), properties.pointOfSale(), certificateStrategy(), false, null,
                    "AFIP integration is disabled");
        }

        if (properties.mode() == AfipMode.SANDBOX) {
            return new AfipHealthResponse(true, properties.mode(), properties.wsEnvironment(), properties.service(),
                    properties.cuit(), properties.pointOfSale(), certificateStrategy(), false, null,
                    "Sandbox mode does not require WSAA token validation");
        }

        validateCredentialConfiguration();
        WsaaLoginResponse token = forceTokenRefresh ? tokenService.refreshToken() : tokenService.currentToken();
        return new AfipHealthResponse(true, properties.mode(), properties.wsEnvironment(), properties.service(),
                properties.cuit(), properties.pointOfSale(), certificateStrategy(), true, token.expirationTime(),
                "WSAA token acquired successfully");
    }

    private void validateCredentialConfiguration() {
        boolean hasPkcs12 = properties.pkcs12Path() != null && !properties.pkcs12Path().isBlank();
        boolean hasPemPair = properties.certificatePath() != null && !properties.certificatePath().isBlank()
                && properties.privateKeyPath() != null && !properties.privateKeyPath().isBlank();
        if (!hasPkcs12 && !hasPemPair) {
            throw new BadRequestException("AFIP production mode requires PKCS12 or certificate/private key paths");
        }
    }

    private String certificateStrategy() {
        if (properties.pkcs12Path() != null && !properties.pkcs12Path().isBlank()) {
            return "PKCS12";
        }
        if (properties.certificatePath() != null && !properties.certificatePath().isBlank()) {
            return "PEM_PAIR";
        }
        return "UNCONFIGURED";
    }
}

