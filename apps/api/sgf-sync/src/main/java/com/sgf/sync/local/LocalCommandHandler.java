package com.sgf.sync.local;

import com.fasterxml.jackson.databind.ObjectMapper;

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
        // In a full implementation, this would write to the local mirror tables
        // For now, the sync queue itself serves as the local persistence layer
        // Mirror tables (local_products, local_sales) can be populated from the queue replay
    }

    public record WriteCommand(
            String aggregateType,
            String aggregateId,
            String eventType,
            Object payload
    ) {
    }
}