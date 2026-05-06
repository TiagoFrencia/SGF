package com.sgf.modules.integrations.afip.wsaa;

import com.sgf.modules.integrations.afip.service.AfipProperties;
import com.sgf.modules.integrations.afip.wsaa.AfipEndpoints;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class AfipTokenService {

    private final AfipProperties properties;
    private final CmsSigner cmsSigner;
    private final WsaaSoapClient wsaaSoapClient;

    private volatile WsaaLoginResponse cachedToken;

    public AfipTokenService(AfipProperties properties, CmsSigner cmsSigner, WsaaSoapClient wsaaSoapClient) {
        this.properties = properties;
        this.cmsSigner = cmsSigner;
        this.wsaaSoapClient = wsaaSoapClient;
    }

    public synchronized WsaaLoginResponse currentToken() {
        if (cachedToken != null && cachedToken.expirationTime().isAfter(OffsetDateTime.now().plusMinutes(2))) {
            return cachedToken;
        }
        return refreshToken();
    }

    public synchronized WsaaLoginResponse refreshToken() {
        String loginTicketRequest = LoginTicketRequestBuilder.build(properties.service(), OffsetDateTime.now());
        String cms = cmsSigner.sign(loginTicketRequest);
        cachedToken = wsaaSoapClient.login(AfipEndpoints.wsaa(properties.wsEnvironment()), cms);
        return cachedToken;
    }
}
