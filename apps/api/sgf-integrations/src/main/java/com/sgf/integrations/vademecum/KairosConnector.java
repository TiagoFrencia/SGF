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
 * REST client for the Kairos pharmaceutical vademecum API.
 *
 * Kairos is the second major drug database in Argentina, complementing AlfaBeta.
 * It specializes in:
 * - Updated pricing (PAMI and standard retail)
 * - Biologic and high-cost medication data
 * - Oncological treatment protocols
 * - Drug interaction database (more granular severity levels)
 * - Bioequivalence and interchangeability data
 * - Monodrug lists for generic substitution compliance
 *
 * Base URL is configurable via application properties.
 */
@Component
public class KairosConnector {

    private static final Logger log = LoggerFactory.getLogger(KairosConnector.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${sgf.vademecum.kairos.base-url:https://api.kairos.com.ar/v1}")
    private String baseUrl;

    @Value("${sgf.vademecum.kairos.api-key:}")
    private String apiKey;

    @Value("${sgf.vademecum.kairos.client-id:}")
    private String clientId;

    public KairosConnector(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch full product catalog updates since a given date.
     */
    public List<KairosProduct> fetchDailyUpdates(int offset, int limit) {
        try {
            String url = baseUrl + "/catalog/updates?offset=" + offset + "&limit=" + limit;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-API-Key", apiKey)
                    .header("X-Client-Id", clientId)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Kairos API returned {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<KairosProduct> products = new ArrayList<>();
            for (JsonNode node : root.get("products")) {
                products.add(parseProduct(node));
            }
            return products;
        } catch (Exception e) {
            log.error("Failed to fetch Kairos updates: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Look up a single product by GTIN in Kairos.
     */
    public Optional<KairosProduct> findByGtin(String gtin) {
        try {
            String url = baseUrl + "/catalog/" + gtin;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-API-Key", apiKey)
                    .header("X-Client-Id", clientId)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) return Optional.empty();
            if (response.statusCode() != 200) {
                log.warn("Kairos lookup {} returned {}", gtin, response.statusCode());
                return Optional.empty();
            }

            return Optional.of(parseProduct(objectMapper.readTree(response.body())));
        } catch (Exception e) {
            log.error("Kairos lookup failed for {}: {}", gtin, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get drug interactions for a list of active ingredients.
     * Kairos provides severity levels: MILD, MODERATE, SEVERE, CONTRAINDICATED.
     */
    public List<DrugInteraction> checkInteractions(List<String> activeIngredients) {
        try {
            String payload = objectMapper.writeValueAsString(
                    java.util.Map.of("ingredients", activeIngredients)
            );

            String url = baseUrl + "/interactions/check";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-API-Key", apiKey)
                    .header("X-Client-Id", clientId)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Kairos interactions returned {}", response.statusCode());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<DrugInteraction> interactions = new ArrayList<>();
            for (JsonNode node : root.get("interactions")) {
                interactions.add(new DrugInteraction(
                        node.get("ingredient_a").asText(),
                        node.get("ingredient_b").asText(),
                        DrugInteractionSeverity.valueOf(node.get("severity").asText()),
                        node.path("description").asText(null),
                        node.path("recommendation").asText(null)
                ));
            }
            return interactions;
        } catch (Exception e) {
            log.error("Kairos interaction check failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get bioequivalent alternatives for a branded product (monodrug list).
     */
    public List<KairosProduct> getBioequivalentAlternatives(String gtin, int limit) {
        try {
            String url = baseUrl + "/catalog/" + gtin + "/bioequivalent?limit=" + limit;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-API-Key", apiKey)
                    .header("X-Client-Id", clientId)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return List.of();

            JsonNode root = objectMapper.readTree(response.body());
            List<KairosProduct> alternatives = new ArrayList<>();
            for (JsonNode node : root.get("products")) {
                alternatives.add(parseProduct(node));
            }
            return alternatives;
        } catch (Exception e) {
            log.error("Kairos bioequivalent search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private KairosProduct parseProduct(JsonNode node) {
        return new KairosProduct(
                node.get("gtin").asText(),
                node.path("commercial_name").asText(null),
                node.path("active_ingredient").asText(null),
                node.path("atc_code").asText(null),
                node.path("pharmaceutical_form").asText(null),
                node.path("concentration").asText(null),
                node.path("is_bioequivalent").asBoolean(false),
                node.path("pami_price").asText(null),
                node.path("retail_price").asText(null),
                node.path("laboratory").asText(null)
        );
    }

    // --- DTOs ---

    public record KairosProduct(
            String gtin,
            String commercialName,
            String activeIngredient,
            String atcCode,
            String pharmaceuticalForm,
            String concentration,
            boolean isBioequivalent,
            String pamiPrice,
            String retailPrice,
            String laboratory
    ) {}

    /**
     * Drug interaction record from Kairos.
     */
    public record DrugInteraction(
            String ingredientA,
            String ingredientB,
            DrugInteractionSeverity severity,
            String description,
            String recommendation
    ) {}

    public enum DrugInteractionSeverity {
        MILD, MODERATE, SEVERE, CONTRAINDICATED
    }
}