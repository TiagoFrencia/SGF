package com.sgf.modules.integrations.afip.wsfe;

public final class WsfeSoapEnvelopeBuilder {

    private WsfeSoapEnvelopeBuilder() {
    }

    public static String buildFeCaeSolicitar(WsfeAuthorizeRequest request) {
        int cbteTipo = invoiceTypeCode(request.invoiceType());
        int docTipo = documentTypeCode(request.documentType());

        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ar="http://ar.gov.afip.dif.FEV1/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <ar:FECAESolicitar>
                      <ar:Auth>
                        <ar:Token>%s</ar:Token>
                        <ar:Sign>%s</ar:Sign>
                        <ar:Cuit>%s</ar:Cuit>
                      </ar:Auth>
                      <ar:FeCAEReq>
                        <ar:FeCabReq>
                          <ar:CantReg>1</ar:CantReg>
                          <ar:PtoVta>%d</ar:PtoVta>
                          <ar:CbteTipo>%d</ar:CbteTipo>
                        </ar:FeCabReq>
                        <ar:FeDetReq>
                          <ar:FECAEDetRequest>
                            <ar:Concepto>1</ar:Concepto>
                            <ar:DocTipo>%d</ar:DocTipo>
                            <ar:DocNro>%s</ar:DocNro>
                            <ar:CbteDesde>%d</ar:CbteDesde>
                            <ar:CbteHasta>%d</ar:CbteHasta>
                            <ar:CbteFch>%s</ar:CbteFch>
                            <ar:ImpTotal>%s</ar:ImpTotal>
                            <ar:ImpTotConc>0.00</ar:ImpTotConc>
                            <ar:ImpNeto>%s</ar:ImpNeto>
                            <ar:ImpOpEx>0.00</ar:ImpOpEx>
                            <ar:ImpIVA>0.00</ar:ImpIVA>
                            <ar:ImpTrib>0.00</ar:ImpTrib>
                            <ar:MonId>%s</ar:MonId>
                            <ar:MonCotiz>1.00</ar:MonCotiz>
                          </ar:FECAEDetRequest>
                        </ar:FeDetReq>
                      </ar:FeCAEReq>
                    </ar:FECAESolicitar>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                request.token(),
                request.sign(),
                request.cuit(),
                request.pointOfSale(),
                cbteTipo,
                docTipo,
                request.documentNumber(),
                request.voucherNumberFrom(),
                request.voucherNumberTo(),
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                request.totalAmount(),
                request.totalAmount(),
                request.currencyCode()
        );
    }

    public static String buildCompUltimoAutorizado(WsfeLastAuthorizedRequest request) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ar="http://ar.gov.afip.dif.FEV1/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <ar:FECompUltimoAutorizado>
                      <ar:Auth>
                        <ar:Token>%s</ar:Token>
                        <ar:Sign>%s</ar:Sign>
                        <ar:Cuit>%s</ar:Cuit>
                      </ar:Auth>
                      <ar:PtoVta>%d</ar:PtoVta>
                      <ar:CbteTipo>%d</ar:CbteTipo>
                    </ar:FECompUltimoAutorizado>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                request.token(),
                request.sign(),
                request.cuit(),
                request.pointOfSale(),
                invoiceTypeCode(request.invoiceType())
        );
    }

    static int invoiceTypeCode(com.sgf.modules.integrations.afip.domain.AfipInvoiceType invoiceType) {
        return switch (invoiceType) {
            case FACTURA_A -> 1;
            case FACTURA_B -> 6;
            case TICKET -> 83;
        };
    }

    static int documentTypeCode(com.sgf.modules.integrations.afip.domain.AfipDocumentType documentType) {
        return switch (documentType) {
            case DNI -> 96;
            case CUIT -> 80;
            case CONSUMIDOR_FINAL -> 99;
        };
    }
}
