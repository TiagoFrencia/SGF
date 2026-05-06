package com.sgf.core.port;

/**
 * Port for sync operations. Implementations include:
 * - SyncLocalService: persists to local SQLite for offline
 * - SyncRemoteService: sends to remote API
 */
public interface SyncPort {

    void enqueue(SyncCommand command);

    record SyncCommand(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload
    ) {
    }
}