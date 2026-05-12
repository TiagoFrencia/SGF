package com.sgf.integrations.vademecum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicMsalVademecumProvider implements VademecumProvider {

    public static final String PROVIDER_CODE = "CNPM_MSAL";

    private static final Logger log = LoggerFactory.getLogger(PublicMsalVademecumProvider.class);
    private static final DateTimeFormatter ARG_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${sgf.vademecum.public-msal.base-url:https://cnpm.msal.gov.ar/api}")
    private String baseUrl;

    public PublicMsalVademecumProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public List<VademecumProduct> search(String searchData) {
        if (searchData == null || searchData.isBlank()) {
            return List.of();
        }
        try {
            String payload = objectMapper.writeValueAsString(
                    java.util.Map.of("searchdata", searchData.trim()));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/vademecum"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Public MSal vademecum returned {} for seed {}", response.statusCode(), searchData);
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<VademecumProduct> products = new ArrayList<>();
            for (JsonNode node : root) {
                products.add(parseProduct(node));
            }
            return products;
        } catch (Exception e) {
            log.error("Public MSal vademecum search failed for {}: {}", searchData, e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<LocalDate> currentVigencia() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/vigencia"))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            return parseDate(root.asText());
        } catch (Exception e) {
            log.warn("Public MSal vigencia lookup failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    VademecumProduct parseProduct(JsonNode node) {
        BigDecimal retailPrice = decimal(node, "PRECIO");
        BigDecimal pamiReferencePrice = decimal(node, "PRECIOPAMI");
        Integer pamiDiscountCode = integer(node, "C_PAMI");
        BigDecimal pamiAffiliatePrice = calculatePamiAffiliatePrice(retailPrice, pamiReferencePrice, pamiDiscountCode);
        LocalDate effectiveDate = parseDate(text(node, "FECHA")).orElse(null);

        return new VademecumProduct(
                text(node, "CLAVE"),
                normalizeGtin(text(node, "GTIN1")),
                cleanBlank(text(node, "TROQUEL")),
                cleanBlank(text(node, "C_BARRA")),
                cleanBlank(text(node, "NOMBRE")),
                cleanBlank(text(node, "PRESENTACION")),
                cleanBlank(text(node, "LABORATORIO")),
                cleanBlank(text(node, "C_LABORATORIO")),
                cleanBlank(text(node, "DROGA")),
                cleanBlank(text(node, "SNOMED")),
                cleanBlank(text(node, "TIPO_DE_VENTA")),
                cleanBlank(text(node, "FORMA")),
                integer(node, "UNIDADES"),
                retailPrice,
                pamiAffiliatePrice,
                pamiDiscountCode,
                cleanBlank(text(node, "D_PAMI")),
                effectiveDate
        );
    }

    BigDecimal calculatePamiAffiliatePrice(BigDecimal retailPrice, BigDecimal pamiReferencePrice, Integer discountCode) {
        if (discountCode == null || discountCode == 0) {
            return null;
        }
        BigDecimal multiplier = switch (discountCode) {
            case 7 -> new BigDecimal("0.60");
            case 2 -> new BigDecimal("0.50");
            case 8 -> new BigDecimal("0.40");
            case 6 -> new BigDecimal("0.20");
            case 4 -> BigDecimal.ZERO;
            default -> BigDecimal.ONE;
        };
        BigDecimal base = pamiReferencePrice != null && pamiReferencePrice.compareTo(BigDecimal.ZERO) > 0
                ? pamiReferencePrice
                : retailPrice;
        if (base == null) {
            return null;
        }
        return base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.trim(), ARG_DATE));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        String text = value.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        return new BigDecimal(text.trim());
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isInt()) {
            return value.asInt();
        }
        String text = value.asText();
        if (text == null || text.isBlank() || "undefined".equalsIgnoreCase(text)) {
            return null;
        }
        return Integer.valueOf(text.trim());
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String cleanBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeGtin(String value) {
        String cleaned = cleanBlank(value);
        if (cleaned == null) {
            return null;
        }
        String digits = cleaned.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() > 14) {
            return digits.substring(digits.length() - 14);
        }
        return "0".repeat(14 - digits.length()) + digits;
    }
}
