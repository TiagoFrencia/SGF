package com.sgf.sync.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;

/**
 * CQRS Write-Side: persists commands locally first,
 * then enqueues them for async remote sync.
 * <p>
 * This is the core of the offline-first engine:
 * - Every write operation hits SQLite first (always succeeds)
 * - A background process replays events to the remote PostgreSQL
 * - Conflict resolution uses last-write-wins + audit log
 */
public class LocalCommandHandler {

    private final LocalDatabase localDatabase;
    private final LocalSyncQueue syncQueue;
    private final ObjectMapper objectMapper;

    public LocalCommandHandler(LocalDatabase localDatabase, LocalSyncQueue syncQueue, ObjectMapper objectMapper) {
        this.localDatabase = localDatabase;
        this.syncQueue = syncQueue;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes a command locally and enqueues it for remote sync.
     */
    public void handle(WriteCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(command.payload());
            // 1. Always write locally first (offline-first guarantee)
            persistLocally(command);
            // 2. Enqueue for async remote sync
            syncQueue.enqueue(
                    command.aggregateType(),
                    command.aggregateId(),
                    command.eventType(),
                    payload
            );
        } catch (Exception e) {
            throw new RuntimeException("Local command handling failed", e);
        }
    }

    private void persistLocally(WriteCommand command) {
        var payloadNode = objectMapper.valueToTree(command.payload());
        String eventType = command.eventType();

        if ("PRODUCT_UPSERTED".equals(eventType) || "PRODUCT_CREATED".equals(eventType)) {
            persistLocalProduct(command.aggregateId(), payloadNode);
            return;
        }

        if ("SALE_CREATED".equals(eventType) || "SALE_COMPLETED".equals(eventType)) {
            persistLocalSale(command.aggregateId(), payloadNode);
        }
    }

    private void persistLocalProduct(String aggregateId, com.fasterxml.jackson.databind.JsonNode payloadNode) {
        String sql = """
                INSERT INTO local_products (
                    id, gtin, sku, commercial_name, brand, active_ingredient, prescription_required, updated_at, synced
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                ON CONFLICT(id) DO UPDATE SET
                    gtin = excluded.gtin,
                    sku = excluded.sku,
                    commercial_name = excluded.commercial_name,
                    brand = excluded.brand,
                    active_ingredient = excluded.active_ingredient,
                    prescription_required = excluded.prescription_required,
                    updated_at = excluded.updated_at,
                    synced = 0
                """;

        try (Connection conn = localDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, aggregateId);
            ps.setString(2, text(payloadNode, "gtin"));
            ps.setString(3, text(payloadNode, "sku"));
            ps.setString(4, text(payloadNode, "commercialName"));
            ps.setString(5, text(payloadNode, "brand"));
            ps.setString(6, text(payloadNode, "activeIngredient"));
            ps.setInt(7, bool(payloadNode, "prescriptionRequired") ? 1 : 0);
            ps.setString(8, OffsetDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist local product", e);
        }
    }

    private void persistLocalSale(String aggregateId, com.fasterxml.jackson.databind.JsonNode payloadNode) {
        String sql = """
                INSERT INTO local_sales (
                    id, external_idempotency_key, total_amount, status, sold_at, synced, updated_at
                ) VALUES (?, ?, ?, ?, ?, 0, ?)
                ON CONFLICT(id) DO UPDATE SET
                    total_amount = excluded.total_amount,
                    status = excluded.status,
                    sold_at = excluded.sold_at,
                    synced = 0,
                    updated_at = excluded.updated_at
                """;

        try (Connection conn = localDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, aggregateId);
            ps.setString(2, text(payloadNode, "externalIdempotencyKey"));
            ps.setBigDecimal(3, payloadNode.path("totalAmount").decimalValue());
            ps.setString(4, text(payloadNode, "status"));
            ps.setString(5, text(payloadNode, "soldAt"));
            ps.setString(6, OffsetDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist local sale", e);
        }
    }

    private String text(com.fasterxml.jackson.databind.JsonNode node, String field) {
        return node.path(field).isMissingNode() || node.path(field).isNull() ? null : node.path(field).asText();
    }

    private boolean bool(com.fasterxml.jackson.databind.JsonNode node, String field) {
        return node.path(field).asBoolean(false);
    }

    public record WriteCommand(
            String aggregateType,
            String aggregateId,
            String eventType,
            Object payload
    ) {
    }
}