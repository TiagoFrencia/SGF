package com.sgf.sync.local;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists domain events to the local SQLite sync queue.
 * These events will be replayed to the remote server when connectivity is restored.
 */
public class LocalSyncQueue {

    private final LocalDatabase localDatabase;
    private final ObjectMapper objectMapper;

    public LocalSyncQueue(LocalDatabase localDatabase, ObjectMapper objectMapper) {
        this.localDatabase = localDatabase;
        this.objectMapper = objectMapper;
    }

    public void enqueue(String aggregateType, String aggregateId, String eventType, String payload) {
        String sql = "INSERT INTO local_sync_queue (id, aggregate_type, aggregate_id, event_type, payload_json, status, created_at) VALUES (?, ?, ?, ?, ?, 'PENDING', ?)";
        try (Connection conn = localDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, aggregateType);
            ps.setString(3, aggregateId);
            ps.setString(4, eventType);
            ps.setString(5, payload);
            ps.setString(6, OffsetDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to enqueue sync event", e);
        }
    }

    public List<SyncEntry> pendingEntries(int limit) {
        List<SyncEntry> entries = new ArrayList<>();
        String sql = "SELECT id, aggregate_type, aggregate_id, event_type, payload_json, created_at, retries FROM local_sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT ?";
        try (Connection conn = localDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new SyncEntry(
                            rs.getString("id"),
                            rs.getString("aggregate_type"),
                            rs.getString("aggregate_id"),
                            rs.getString("event_type"),
                            rs.getString("payload_json"),
                            rs.getString("created_at"),
                            rs.getInt("retries")
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch pending sync entries", e);
        }
        return entries;
    }

    public void markProcessed(String entryId) {
        executeStatusUpdate(entryId, "PROCESSED", null);
    }

    public void markFailed(String entryId, String error) {
        String sql = "UPDATE local_sync_queue SET retries = retries + 1, last_error = ?, status = CASE WHEN retries + 1 >= 5 THEN 'DEAD' ELSE 'PENDING' END WHERE id = ?";
        try (Connection conn = localDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, error);
            ps.setString(2, entryId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to mark sync entry as failed", e);
        }
    }

    private void executeStatusUpdate(String entryId, String status, String processedAt) {
        String sql = "UPDATE local_sync_queue SET status = ?, processed_at = ? WHERE id = ?";
        try (Connection conn = localDatabase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, processedAt != null ? processedAt : OffsetDateTime.now().toString());
            ps.setString(3, entryId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update sync entry status", e);
        }
    }

    public record SyncEntry(
            String id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            String createdAt,
            int retries
    ) {
    }
}