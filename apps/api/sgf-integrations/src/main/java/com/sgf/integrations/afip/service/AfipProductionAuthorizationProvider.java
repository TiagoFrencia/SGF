package com.sgf.integrations.afip.service;

import com.sgf.integrations.afip.wsaa.AfipEndpoints;
import com.sgf.integrations.afip.wsaa.AfipTokenService;
import com.sgf.integrations.afip.wsfe.WsfeAuthorizeRequest;
import com.sgf.integrations.afip.wsfe.WsfeAuthorizeResponse;
import com.sgf.integrations.afip.wsfe.WsfeLastAuthorizedRequest;
import com.sgf.integrations.afip.wsfe.WsfeLastAuthorizedResponse;
import com.sgf.integrations.afip.wsfe.WsfeSoapClient;
import org.springframework.stereotype.Component;

@Component
public class AfipProductionAuthorizationProvider implements AfipAuthorizationProvider {

    private final AfipProperties properties;
    private final AfipTokenService tokenService;
    private final WsfeSoapClient wsfeSoapClient;

    public AfipProductionAuthorizationProvider(AfipProperties properties,
                                               AfipTokenService tokenService,
                                               WsfeSoapClient wsfeSoapClient) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.wsfeSoapClient = wsfeSoapClient;
    }

    @Override
    public AfipMode mode() {
        return AfipMode.PRODUCTION;
    }

    @Override
    public AfipAuthorizationResult authorize(AfipAuthorizationCommand command) {
        var token = tokenService.currentToken();
        String endpoint = AfipEndpoints.wsfe(properties.wsEnvironment());
        WsfeLastAuthorizedResponse lastAuthorized = wsfeSoapClient.lastAuthorized(
                endpoint,
                new WsfeLastAuthorizedRequest(
                        token.token(),
                        token.sign(),
                        command.cuit(),
                        command.pointOfSale(),
                        command.invoiceType()
                )
        );
        long nextVoucherNumber = lastAuthorized.lastAuthorizedNumber() + 1;
        WsfeAuthorizeResponse response = wsfeSoapClient.authorize(
                endpoint,
                new WsfeAuthorizeRequest(
                        token.token(),
                        token.sign(),
                        command.cuit(),
                        command.pointOfSale(),
                        command.invoiceType(),
                        command.customerDocumentType(),
                        command.customerDocumentNumber(),
                        command.totalAmount(),
                        command.currencyCode(),
                        nextVoucherNumber,
                        nextVoucherNumber
                )
        );
        return new AfipAuthorizationResult(
                response.status(),
                nextVoucherNumber,
                nextVoucherNumber,
                response.resultCode(),
                response.cae(),
                response.caeDueDate(),
                properties.wsEnvironment().name().toLowerCase() + "-wsfe",
                response.rawXml(),
                response.observations(),
                response.errors(),
                token.expirationTime()
        );
    }
}
