package com.sgf.integrations.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.integrations.anmat.service.AnmatMode;
import com.sgf.integrations.anmat.service.AnmatProperties;
import com.sgf.integrations.anmat.service.AnmatTraceabilityGateway;
import com.sgf.integrations.anmat.service.TraceabilityReportCommand;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AnmatAdapter implements ExternalIntegrationPort, AnmatTraceabilityGateway {

    private final AnmatProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AnmatAdapter(AnmatProperties properties,
                        ObjectMapper objectMapper,
                        RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeoutMillis());
        requestFactory.setReadTimeout(properties.timeoutMillis());
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String integrationCode() {
        return "ANMAT";
    }

    @Override
    public String status() {
        if (!properties.enabled()) {
            return "DISABLED";
        }
        return properties.mode().name();
    }

    @Override
    public GatewayResult report(TraceabilityReportCommand command) {
        if (!properties.enabled()) {
            return new GatewayResult(false, null, "ANMAT integration is disabled", null, null, false, "DISABLED");
        }
        return properties.mode() == AnmatMode.SANDBOX ? sandbox(command) : production(command);
    }

    private GatewayResult sandbox(TraceabilityReportCommand command) {
        String providerReference = "anmat-sandbox-" + UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("integration", "ANMAT");
        payload.put("mode", "SANDBOX");
        payload.put("providerReference", providerReference);
        payload.put("eventType", command.eventType().name());
        payload.put("gtin", command.gtin());
        payload.put("serialNumber", command.serialNumber());
        payload.put("occurredAt", command.occurredAt());
        return new GatewayResult(true, toJson(payload), null, providerReference, 200, false, "SANDBOX");
    }

    private GatewayResult production(TraceabilityReportCommand command) {
        String requestBody = toJson(buildProductionPayload(command));
        try {
            String responseBody = restClient.post()
                    .uri(URI.create(properties.baseUrl() + properties.reportPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.setBasicAuth(properties.username(), properties.password());
                        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
                    })
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            return new GatewayResult(
                    true,
                    responseBody,
                    null,
                    extractProviderReference(responseBody),
                    200,
                    false,
                    "PRODUCTION"
            );
        } catch (RestClientResponseException ex) {
            return new GatewayResult(
                    false,
                    ex.getResponseBodyAsString(),
                    "ANMAT request failed with HTTP %s".formatted(ex.getStatusCode().value()),
                    null,
                    ex.getStatusCode().value(),
                    ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429,
                    "PRODUCTION"
            );
        } catch (ResourceAccessException ex) {
            return new GatewayResult(
                    false,
                    null,
                    "ANMAT endpoint could not be reached: " + ex.getMessage(),
                    null,
                    null,
                    true,
                    "PRODUCTION"
            );
        }
    }

    private Map<String, Object> buildProductionPayload(TraceabilityReportCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", command.eventType().name());
        payload.put("gtin", command.gtin());
        payload.put("serialNumber", command.serialNumber());
        payload.put("lotNumber", command.lotNumber());
        payload.put("expiresAt", command.expiresAt());
        payload.put("occurredAt", command.occurredAt());
        payload.put("gln", command.gln() != null ? command.gln() : properties.establishmentGln());
        payload.put("establishmentGln", properties.establishmentGln());
        payload.put("productId", command.productId());
        payload.put("batchId", command.batchId());
        payload.put("saleId", command.saleId());
        payload.put("submittedAt", OffsetDateTime.now());
        return payload;
    }

    private String extractProviderReference(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            if (node.hasNonNull("providerReference")) {
                return node.get("providerReference").asText();
            }
            if (node.hasNonNull("transactionId")) {
                return node.get("transactionId").asText();
            }
            if (node.hasNonNull("id")) {
                return node.get("id").asText();
            }
            return null;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize ANMAT payload", ex);
        }
    }
}
