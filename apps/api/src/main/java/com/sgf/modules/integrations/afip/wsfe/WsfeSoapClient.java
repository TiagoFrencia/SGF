package com.sgf.modules.integrations.afip.wsfe;

import com.sgf.modules.integrations.afip.domain.AfipInvoiceStatus;
import com.sgf.modules.integrations.afip.service.AfipMessage;
import com.sgf.modules.integrations.afip.service.AfipProviderException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class WsfeSoapClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public WsfeAuthorizeResponse authorize(String endpoint, WsfeAuthorizeRequest request) {
        String payload = WsfeSoapEnvelopeBuilder.buildFeCaeSolicitar(request);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "\"http://ar.gov.afip.dif.FEV1/FECAESolicitar\"")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new AfipProviderException("HTTP_" + response.statusCode(), "WSFE returned status " + response.statusCode(), false, response.body());
            }
            return parse(response.body());
        } catch (HttpTimeoutException ex) {
            throw new AfipProviderException("WSFE_TIMEOUT", "WSFE authorization timed out after request submission; verify duplicate state before retrying", false, null);
        } catch (Exception ex) {
            if (ex instanceof AfipProviderException providerException) {
                throw providerException;
            }
            throw new AfipProviderException("WSFE_TRANSPORT", "WSFE authorization failed after request submission; verify duplicate state before retrying: " + ex.getMessage(), false, null);
        }
    }

    public WsfeLastAuthorizedResponse lastAuthorized(String endpoint, WsfeLastAuthorizedRequest request) {
        String payload = WsfeSoapEnvelopeBuilder.buildCompUltimoAutorizado(request);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "\"http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado\"")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new AfipProviderException(
                        "HTTP_" + response.statusCode(),
                        "WSFE last authorized returned status " + response.statusCode(),
                        response.statusCode() >= 500 || response.statusCode() == 429,
                        response.body()
                );
            }
            return parseLastAuthorized(response.body());
        } catch (HttpTimeoutException ex) {
            throw new AfipProviderException("WSFE_LAST_TIMEOUT", "WSFE last authorized timed out", true, null);
        } catch (Exception ex) {
            if (ex instanceof AfipProviderException providerException) {
                throw providerException;
            }
            throw new AfipProviderException("WSFE_LAST_TRANSPORT", "WSFE last authorized lookup failed: " + ex.getMessage(), true, null);
        }
    }

    private WsfeAuthorizeResponse parse(String xml) throws Exception {
        Document doc = parseXml(xml);
        assertNoSoapFault(doc, xml);
        String cae = optionalTextOf(doc, "CAE");
        String caeDue = optionalTextOf(doc, "CAEFchVto");
        String result = optionalTextOf(doc, "Resultado");
        List<AfipMessage> observations = extractMessages(doc, "Obs");
        List<AfipMessage> errors = extractMessages(doc, "Err");
        AfipInvoiceStatus status = "A".equalsIgnoreCase(result) ? AfipInvoiceStatus.AUTHORIZED : AfipInvoiceStatus.REJECTED;
        LocalDate dueDate = caeDue == null || caeDue.isBlank()
                ? null
                : LocalDate.parse(caeDue, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return new WsfeAuthorizeResponse(status, result, cae, dueDate, xml, observations, errors);
    }

    private Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String optionalTextOf(Document document, String tagName) {
        var nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private WsfeLastAuthorizedResponse parseLastAuthorized(String xml) throws Exception {
        Document doc = parseXml(xml);
        assertNoSoapFault(doc, xml);
        String value = optionalTextOf(doc, "CbteNro");
        long last = value == null || value.isBlank() ? 0L : Long.parseLong(value);
        return new WsfeLastAuthorizedResponse(last, xml);
    }

    private void assertNoSoapFault(Document doc, String xml) {
        String faultString = optionalTextOf(doc, "faultstring");
        if (faultString != null && !faultString.isBlank()) {
            throw new AfipProviderException("SOAP_FAULT", faultString, false, xml);
        }
    }

    private List<AfipMessage> extractMessages(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        List<AfipMessage> messages = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            if (nodes.item(index) instanceof Element element) {
                String code = firstText(element, "Code");
                String message = firstText(element, "Msg");
                messages.add(new AfipMessage(code, message));
            }
        }
        return messages;
    }

    private String firstText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }
}
