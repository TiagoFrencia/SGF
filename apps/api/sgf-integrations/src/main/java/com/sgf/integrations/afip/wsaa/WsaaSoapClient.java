package com.sgf.integrations.afip.wsaa;

import com.sgf.integrations.afip.service.AfipProviderException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.OffsetDateTime;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@Component
public class WsaaSoapClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public WsaaLoginResponse login(String endpoint, String cmsBase64) {
        String payload = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsaa="http://wsaa.view.sua.dvadac.desein.afip.gov">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <wsaa:loginCms>
                      <wsaa:in0>%s</wsaa:in0>
                    </wsaa:loginCms>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(cmsBase64);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "\"\"")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new AfipProviderException("WSAA_HTTP_" + response.statusCode(), "WSAA returned status " + response.statusCode(), response.statusCode() >= 500, response.body());
            }
            return parse(response.body());
        } catch (HttpTimeoutException ex) {
            throw new AfipProviderException("WSAA_TIMEOUT", "WSAA login timed out", true, null);
        } catch (Exception ex) {
            if (ex instanceof AfipProviderException providerException) {
                throw providerException;
            }
            throw new AfipProviderException("WSAA_TRANSPORT", "WSAA login failed: " + ex.getMessage(), true, null);
        }
    }

    private WsaaLoginResponse parse(String soapXml) throws Exception {
        Document doc = parseXml(soapXml);
        String faultString = optionalTextOf(doc, "faultstring");
        if (faultString != null && !faultString.isBlank()) {
            throw new AfipProviderException("WSAA_SOAP_FAULT", faultString, false, soapXml);
        }
        String loginTicketResponse = textOf(doc, "loginCmsReturn");
        Document inner = parseXml(loginTicketResponse);
        String token = textOf(inner, "token");
        String sign = textOf(inner, "sign");
        OffsetDateTime expiration = OffsetDateTime.parse(textOf(inner, "expirationTime"));
        return new WsaaLoginResponse(token, sign, expiration, loginTicketResponse);
    }

    private Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String textOf(Document document, String tagName) {
        var nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new AfipProviderException("AFIP_PARSE", "AFIP response missing tag " + tagName, false, null);
        }
        return nodes.item(0).getTextContent();
    }

    private String optionalTextOf(Document document, String tagName) {
        var nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }
}
