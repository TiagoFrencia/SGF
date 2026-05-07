package com.sgf.integrations.vademecum;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Embedded WireMock server to simulate pharmaceutical vademecum APIs (AlfaBeta/Kairos).
 * Used for integration testing and local development sandbox mode.
 */
public class VademecumSandboxServer {

    private final WireMockServer server;

    public VademecumSandboxServer(int port) {
        this.server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
    }

    public void start() {
        server.start();
        setupStubs();
    }

    public void stop() {
        server.stop();
    }

    private void setupStubs() {
        // AlfaBeta Product Lookup
        server.stubFor(get(urlMatching("/alfabeta/products/7791234567890"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"gtin\":\"7791234567890\",\"commercial_name\":\"IBUPROFENO 600MG\",\"brand\":\"ACTRON\",\"active_ingredient\":\"IBUPROFENO\",\"atc_code\":\"M01AE01\",\"pharmaceutical_form\":\"TABLETS\",\"concentration\":\"600mg\",\"presentation\":\"30 tablets\",\"laboratory\":\"BAYER\",\"requires_prescription\":true}")));

        // Kairos Interaction Check
        server.stubFor(post(urlEqualTo("/kairos/interactions/check"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"interactions\":[{\"ingredient_a\":\"WARFARINA\",\"ingredient_b\":\"ASPIRINA\",\"severity\":\"SEVERE\",\"description\":\"Aumento del riesgo de sangrado\",\"recommendation\":\"Evitar uso concomitante\"}]}")));
        
        // Kairos Bioequivalent search
        server.stubFor(get(urlMatching("/kairos/catalog/7791234567890/bioequivalent.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"products\":[{\"gtin\":\"7791112223334\",\"commercial_name\":\"IBUPROFENO GENERICO\",\"active_ingredient\":\"IBUPROFENO\",\"is_bioequivalent\":true,\"retail_price\":\"450.00\"}]}")));
    }
}
