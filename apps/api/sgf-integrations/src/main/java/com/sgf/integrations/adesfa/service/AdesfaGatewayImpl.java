package com.sgf.integrations.adesfa.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdesfaGatewayImpl implements AdesfaGateway {

    private final AdesfaProperties properties;
    private final AdesfaValidatorRegistry registry;
    private final HttpClient httpClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public AdesfaGatewayImpl(AdesfaProperties properties,
                             AdesfaValidatorRegistry registry,
                             com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.properties = properties;
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000, properties.timeoutMillis())))
                .build();
    }

    @Override
    public GatewayResult validate(AdesfaValidationCommand command) {
        if (properties.mode() == AdesfaMode.SANDBOX) {
            return simulateValidation(command);
        }

        return validateProduction(command);
    }

    private GatewayResult simulateValidation(AdesfaValidationCommand command) {
        var validator = registry.getValidator(command.validatorCode());
        
        if (validator.isPresent()) {
            // Simplified validation for simulation
            BigDecimal total = command.totalAmount();
            BigDecimal coverage;
            String msg;
            
            switch (command.validatorCode()) {
                case "PAMI" -> {
                    coverage = total.multiply(new BigDecimal("0.70"));
                    msg = "OK_PAMI";
                }
                case "OSDE" -> {
                    coverage = total.multiply(new BigDecimal("0.40"));
                    msg = "OK_OSDE";
                }
                case "SWISS_MEDICAL" -> {
                    coverage = total.multiply(new BigDecimal("0.50"));
                    msg = "OK_SWISS";
                }
                default -> {
                    coverage = BigDecimal.ZERO;
                    msg = "OK_GENERIC";
                }
            }
            
            return new GatewayResult(
                    true,
                    "{\"status\":\"" + msg + "\"}",
                    null,
                    UUID.randomUUID().toString(),
                    200,
                    false,
                    "SANDBOX",
                    total.subtract(coverage),
                    coverage
            );
        }

        return new GatewayResult(
                true,
                "{\"status\":\"OK\"}",
                null,
                UUID.randomUUID().toString(),
                200,
                false,
                "SANDBOX",
                command.totalAmount(),
                BigDecimal.ZERO
        );
    }

    private GatewayResult validateProduction(AdesfaValidationCommand command) {
        try {
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("validatorCode", command.validatorCode());
            payload.put("actionCode", command.actionCode());
            payload.put("affiliateNumber", command.affiliateNumber());
            payload.put("prescriptionNumber", command.prescriptionNumber());
            payload.put("saleId", command.saleId().toString());
            payload.put("totalAmount", command.totalAmount());
            payload.put("requestedAt", command.requestedAt().toString());
            payload.put("softwareCode", properties.softwareCode());
            payload.put("providerCode", properties.providerCode());

            String body = objectMapper.writeValueAsString(payload);
            String url = properties.baseUrl() + properties.validationPath();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(Math.max(2000, properties.timeoutMillis())))
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (properties.username() != null && !properties.username().isBlank()) {
                String basic = java.util.Base64.getEncoder().encodeToString(
                        (properties.username() + ":" + (properties.password() == null ? "" : properties.password()))
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                );
                builder.header("Authorization", "Basic " + basic);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            boolean success = status >= 200 && status < 300;
            boolean retryable = status >= 500 || status == 429;

            BigDecimal coverageAmount = BigDecimal.ZERO;
            BigDecimal patientAmount = command.totalAmount();
            String providerReference = null;
            if (success && response.body() != null && !response.body().isBlank()) {
                var json = objectMapper.readTree(response.body());
                coverageAmount = decimal(json, "coverageAmount", BigDecimal.ZERO);
                patientAmount = decimal(json, "patientAmount", command.totalAmount().subtract(coverageAmount));
                providerReference = text(json, "providerReference");
            }

            return new GatewayResult(
                    success,
                    response.body(),
                    success ? null : "ADESFA provider returned HTTP " + status,
                    providerReference != null ? providerReference : UUID.randomUUID().toString(),
                    status,
                    retryable,
                    "PRODUCTION",
                    patientAmount.max(BigDecimal.ZERO),
                    coverageAmount.max(BigDecimal.ZERO)
            );
        } catch (Exception e) {
            return new GatewayResult(
                    false,
                    null,
                    "ADESFA production call failed: " + e.getMessage(),
                    UUID.randomUUID().toString(),
                    503,
                    true,
                    "PRODUCTION",
                    command.totalAmount(),
                    BigDecimal.ZERO
            );
        }
    }

    private String text(com.fasterxml.jackson.databind.JsonNode node, String field) {
        var value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(com.fasterxml.jackson.databind.JsonNode node, String field, BigDecimal fallback) {
        var value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return fallback;
        try {
            return value.decimalValue();
        } catch (Exception ex) {
            return fallback;
        }
    }
}
