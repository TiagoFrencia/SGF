package com.sgf.integrations.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.integrations.adesfa.service.AdesfaGateway;
import com.sgf.integrations.adesfa.service.AdesfaMode;
import com.sgf.integrations.adesfa.service.AdesfaProperties;
import com.sgf.integrations.adesfa.service.AdesfaValidationCommand;
import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AdesfaAdapter implements ExternalIntegrationPort, AdesfaGateway {

    private final AdesfaProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AdesfaAdapter(AdesfaProperties properties,
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
        return "ADESFA";
    }

    @Override
    public String status() {
        if (!properties.enabled()) {
            return "DISABLED";
        }
        return properties.mode().name();
    }

    @Override
    public GatewayResult validate(AdesfaValidationCommand command) {
        if (!properties.enabled()) {
            return new GatewayResult(false, null, "ADESFA integration is disabled", null, null, false, "DISABLED", BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return properties.mode() == AdesfaMode.SANDBOX ? sandbox(command) : production(command);
    }

    private GatewayResult sandbox(AdesfaValidationCommand command) {
        BigDecimal coverage = command.totalAmount().multiply(new BigDecimal("0.70")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal patient = command.totalAmount().subtract(coverage).setScale(2, java.math.RoundingMode.HALF_UP);
        String providerReference = "adesfa-sandbox-" + UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("integration", "ADESFA");
        payload.put("mode", "SANDBOX");
        payload.put("providerReference", providerReference);
        payload.put("validatorCode", command.validatorCode());
        payload.put("actionCode", command.actionCode());
        payload.put("affiliateNumber", command.affiliateNumber());
        payload.put("prescriptionNumber", command.prescriptionNumber());
        payload.put("patientAmount", patient);
        payload.put("coverageAmount", coverage);
        return new GatewayResult(true, toJson(payload), null, providerReference, 200, false, "SANDBOX", patient, coverage);
    }

    private GatewayResult production(AdesfaValidationCommand command) {
        String requestBody = toJson(buildProductionPayload(command));
        try {
            String responseBody = restClient.post()
                    .uri(URI.create(properties.baseUrl() + properties.validationPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.setBasicAuth(properties.username(), properties.password());
                        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    })
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            BigDecimal patientAmount = extractAmount(responseBody, "patientAmount");
            BigDecimal coverageAmount = extractAmount(responseBody, "coverageAmount");
            return new GatewayResult(
                    true,
                    responseBody,
                    null,
                    extractProviderReference(responseBody),
                    200,
                    false,
                    "PRODUCTION",
                    patientAmount,
                    coverageAmount
            );
        } catch (RestClientResponseException ex) {
            return new GatewayResult(
                    false,
                    ex.getResponseBodyAsString(),
                    "ADESFA request failed with HTTP %s".formatted(ex.getStatusCode().value()),
                    null,
                    ex.getStatusCode().value(),
                    ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429,
                    "PRODUCTION",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        } catch (ResourceAccessException ex) {
            return new GatewayResult(
                    false,
                    null,
                    "ADESFA endpoint could not be reached: " + ex.getMessage(),
                    null,
                    null,
                    true,
                    "PRODUCTION",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }
    }

    private Map<String, Object> buildProductionPayload(AdesfaValidationCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", "3.1.0");
        payload.put("messageType", 200);
        payload.put("actionCode", command.actionCode());
        payload.put("softwareCode", properties.softwareCode());
        payload.put("providerCode", properties.providerCode());
        payload.put("validatorCode", command.validatorCode());
        payload.put("affiliateNumber", command.affiliateNumber());
        payload.put("prescriptionNumber", command.prescriptionNumber());
        payload.put("saleId", command.saleId());
        payload.put("totalAmount", command.totalAmount());
        payload.put("requestedAt", command.requestedAt());
        return payload;
    }

    private BigDecimal extractAmount(String responseBody, String field) {
        if (responseBody == null || responseBody.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            if (node.hasNonNull(field)) {
                return node.get(field).decimalValue();
            }
        } catch (JsonProcessingException ex) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
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
            throw new IllegalStateException("Could not serialize ADESFA payload", ex);
        }
    }
}
