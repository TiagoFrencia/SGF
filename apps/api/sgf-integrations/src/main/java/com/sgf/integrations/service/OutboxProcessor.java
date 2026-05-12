package com.sgf.integrations.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.integrations.afip.domain.AfipDocumentType;
import com.sgf.integrations.afip.domain.AfipInvoiceType;
import com.sgf.integrations.afip.service.AfipProperties;
import com.sgf.integrations.afip.service.AfipService;
import com.sgf.integrations.afip.web.AfipAuthorizeInvoiceRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes pending events in the outbox and relays them to external systems.
 * This is a critical component for system reliability (Phase 4 Hardening).
 */
@Component
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private final OutboxEventRepository outboxEventRepository;
    private final AfipService afipService;
    private final AfipProperties afipProperties;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(OutboxEventRepository outboxEventRepository,
                           AfipService afipService,
                           AfipProperties afipProperties,
                           ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.afipService = afipService;
        this.afipProperties = afipProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Polls pending events every 10 seconds.
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                processEvent(event);
                event.setStatus("COMPLETED");
            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}", event.getId(), e.getMessage());
                event.setStatus("FAILED");
                event.setLastError(e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                
                // Simple backoff logic could go here
            }
            outboxEventRepository.save(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        switch (event.getEventType()) {
            case "SALE_COMPLETED":
                log.info("Relaying SALE_COMPLETED event for aggregate {}", event.getAggregateId());
                handleSaleCompleted(event);
                break;
            case "STOCK_UPDATED":
                log.info("Relaying STOCK_UPDATED event for product {}", event.getAggregateId());
                break;
            default:
                log.warn("Unknown event type in outbox: {}", event.getEventType());
        }
    }

    private void handleSaleCompleted(OutboxEvent event) {
        JsonNode payload = readPayload(event.getPayloadJson());
        logTraceableItemsWithoutBlocking(payload);
        if (!afipProperties.enabled() || !afipProperties.autoInvoiceEnabled()) {
            log.info("AFIP auto invoice disabled; SALE_COMPLETED {} remains relayed only", event.getAggregateId());
            return;
        }
        afipService.authorizeSaleInvoice(
                event.getAggregateId(),
                new AfipAuthorizeInvoiceRequest(
                        AfipInvoiceType.FACTURA_B,
                        documentType(payload.path("customerDocument").asText(null)),
                        documentNumber(payload.path("customerDocument").asText(null)),
                        afipProperties.pointOfSale(),
                        "ARS"
                ),
                "outbox-processor"
        );
    }

    private JsonNode readPayload(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid SALE_COMPLETED outbox payload", ex);
        }
    }

    private void logTraceableItemsWithoutBlocking(JsonNode payload) {
        JsonNode items = payload.path("items");
        if (!items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            if (item.path("requiresTraceability").asBoolean(false)
                    && item.path("batchId").asText("").isBlank()) {
                log.warn("Traceable product {} sold without batch/serial data; ANMAT reporting must be remediated",
                        item.path("productId").asText());
            }
        }
    }

    private AfipDocumentType documentType(String document) {
        String normalized = documentNumber(document);
        if (normalized.length() == 11) {
            return AfipDocumentType.CUIT;
        }
        if ("0".equals(normalized)) {
            return AfipDocumentType.CONSUMIDOR_FINAL;
        }
        return AfipDocumentType.DNI;
    }

    private String documentNumber(String document) {
        if (document == null || document.isBlank()) {
            return "0";
        }
        String normalized = document.replaceAll("\\D", "");
        return normalized.isBlank() ? "0" : normalized;
    }
}
