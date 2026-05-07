package com.sgf.integrations.afip;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sgf.integrations.afip.domain.AfipDocumentType;
import com.sgf.integrations.afip.domain.AfipInvoiceType;
import com.sgf.integrations.afip.wsaa.LoginTicketRequestBuilder;
import com.sgf.integrations.afip.wsfe.WsfeAuthorizeRequest;
import com.sgf.integrations.afip.wsfe.WsfeLastAuthorizedRequest;
import com.sgf.integrations.afip.wsfe.WsfeSoapEnvelopeBuilder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class AfipBuildersTests {

    @Test
    void loginTicketContainsServiceAndTimestamps() {
        String xml = LoginTicketRequestBuilder.build("wsfe", OffsetDateTime.parse("2026-05-04T10:15:30Z"));
        assertTrue(xml.contains("<service>wsfe</service>"));
        assertTrue(xml.contains("<generationTime>"));
        assertTrue(xml.contains("<expirationTime>"));
    }

    @Test
    void wsfeEnvelopeMapsInvoiceAndDocumentTypes() {
        String xml = WsfeSoapEnvelopeBuilder.buildFeCaeSolicitar(new WsfeAuthorizeRequest(
                "token",
                "sign",
                "20123456789",
                1,
                AfipInvoiceType.FACTURA_B,
                AfipDocumentType.DNI,
                "30111222",
                new BigDecimal("1250.00"),
                "ARS",
                12L,
                12L
        ));
        assertTrue(xml.contains("<ar:CbteTipo>6</ar:CbteTipo>"));
        assertTrue(xml.contains("<ar:DocTipo>96</ar:DocTipo>"));
        assertTrue(xml.contains("<ar:ImpTotal>1250.00</ar:ImpTotal>"));
        assertTrue(xml.contains("<ar:CbteDesde>12</ar:CbteDesde>"));
    }

    @Test
    void wsfeLastAuthorizedEnvelopeUsesPointOfSaleAndInvoiceType() {
        String xml = WsfeSoapEnvelopeBuilder.buildCompUltimoAutorizado(new WsfeLastAuthorizedRequest(
                "token",
                "sign",
                "20123456789",
                3,
                AfipInvoiceType.FACTURA_A
        ));
        assertTrue(xml.contains("<ar:PtoVta>3</ar:PtoVta>"));
        assertTrue(xml.contains("<ar:CbteTipo>1</ar:CbteTipo>"));
    }
}
