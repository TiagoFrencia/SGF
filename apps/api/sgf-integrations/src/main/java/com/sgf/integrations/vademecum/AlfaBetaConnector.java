package com.sgf.integrations.vademecum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * REST client for the AlfaBeta pharmaceutical vademecum API.
 *
 * AlfaBeta is one of the two major drug databases in Argentina (along with Kairos).
 * It provides:
 * - Commercial product catalog (names, presentations, prices)
 * - Active ingredient composition (IFA: Ingrediente Farmacéutico Activo)
 * - ATC classification codes
 * - Therapeutic indications
 * - Contraindications and warnings
 * - Drug interaction data
 *
 * Base URL is configurable (sandbox vs production endpoint).
 */
@Component
public class AlfaBetaConnector {

    private static final Logger log = LoggerFactory.getLogger(AlfaBetaConnector.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${sgf.vademecum.alfabeta.base-url:https://api.alfabeta.net/v2}")
    private String baseUrl;

    @Value("${sgf.vademecum.alfabeta.api-key:}")
    private String apiKey;

    public AlfaBetaConnector(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch full catalog delta since a given date.
     */
    public List<AlfaBetaProduct> fetchDailyUpdates(int page, int pageSize) {
        try {
            String url = baseUrl + "/products/updates?page=" + page + "&size=" + pageSize;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-API-Key", apiKey)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("AlfaBeta API returned {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<AlfaBetaProduct> products = new ArrayList<>();
            for (JsonNode node : root.get("data")) {
                products.add(parseProduct(node));
            }
            return products;
        } catch (Exception e) {
            log.error("Failed to fetch AlfaBeta updates: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Look up a single product by GTIN.
     */
    public Optional<AlfaBetaProduct> findByGtin(String gtin) {
        try {
            String url = baseUrl + "/products/" + gtin;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-API-Key", apiKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) return Optional.empty();
            if (response.statusCode() != 200) {
                log.warn("AlfaBeta lookup {} returned {}", gtin, response.statusCode());
                return Optional.empty();
            }

            return Optional.of(parseProduct(objectMapper.readTree(response.body())));
        } catch (Exception e) {
            log.error("AlfaBeta lookup failed for {}: {}", gtin, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Search by active ingredient (IFA).
     */
    public List<AlfaBetaProduct> findByActiveIngredient(String activeIngredient, int limit) {
        try {
            String url = baseUrl + "/products/search?ifa=" + urlEncode(activeIngredient) + "&limit=" + limit;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-API-Key", apiKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return List.of();

            JsonNode root = objectMapper.readTree(response.body());
            List<AlfaBetaProduct> results = new ArrayList<>();
            for (JsonNode node : root.get("data")) {
                results.add(parseProduct(node));
            }
            return results;
        } catch (Exception e) {
            log.error("AlfaBeta IFA search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private AlfaBetaProduct parseProduct(JsonNode node) {
        return new AlfaBetaProduct(
                node.get("gtin").asText(),
                node.get("commercial_name").asText(),
                node.path("brand").asText(null),
                node.path("active_ingredient").asText(null),
                node.path("atc_code").asText(null),
                node.path("pharmaceutical_form").asText(null),
                node.path("concentration").asText(null),
                node.path("presentation").asText(null),
                node.path("laboratory").asText(null),
                node.path("requires_prescription").asBoolean(false),
                node.path("updated_at").asText(null)
        );
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    // --- DTO ---

    /**
     * Product data from the AlfaBeta vademecum.
     */
    public record AlfaBetaProduct(
            String gtin,
            String commercialName,
            String brand,
            String activeIngredient,
            String atcCode,
            String pharmaceuticalForm,
            String concentration,
            String presentation,
            String laboratory,
            boolean requiresPrescription,
            String updatedAt
    ) {}
}