package com.sgf.sync.queue;

import com.sgf.sync.local.LocalSyncQueue;
import com.sgf.sync.local.LocalSyncQueue.SyncEntry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background processor that replays local events to the remote server.
 * <p>
 * Runs on a scheduled interval. When connectivity is available:
 * - Fetches PENDING entries from local SQLite
 * - Sends them to the remote REST API
 * - Marks them PROCESSED on success, or increments retries on failure
 * - Dead-letters entries after 5 failed retries
 */
public class SyncReplayProcessor {

    private static final Logger log = LoggerFactory.getLogger(SyncReplayProcessor.class);

    private final LocalSyncQueue syncQueue;
    private final RemoteSyncClient remoteClient;

    public SyncReplayProcessor(LocalSyncQueue syncQueue, RemoteSyncClient remoteClient) {
        this.syncQueue = syncQueue;
        this.remoteClient = remoteClient;
    }

    /**
     * Attempt to replay all pending events.
     * Call this on a schedule (e.g., every 30 seconds) or on connectivity restoration.
     *
     * @return number of events successfully replayed
     */
    public int replay() {
        if (!remoteClient.isOnline()) {
            log.debug("Remote server unreachable, skipping sync replay");
            return 0;
        }

        List<SyncEntry> pending = syncQueue.pendingEntries(50);
        int replayed = 0;

        for (SyncEntry entry : pending) {
            try {
                remoteClient.send(entry);
                syncQueue.markProcessed(entry.id());
                replayed++;
            } catch (Exception e) {
                log.warn("Sync replay failed for entry {}: {}", entry.id(), e.getMessage());
                syncQueue.markFailed(entry.id(), e.getMessage());
            }
        }

        if (replayed > 0) {
            log.info("Replayed {} sync events to remote", replayed);
        }
        return replayed;
    }

    /**
     * Force replay of all pending entries (e.g., on manual sync trigger).
     */
    public SyncReplayResult forceReplay() {
        if (!remoteClient.isOnline()) {
            return new SyncReplayResult(0, 0, "Remote server unreachable");
        }

        List<SyncEntry> pending = syncQueue.pendingEntries(500);
        int succeeded = 0;
        int failed = 0;

        for (SyncEntry entry : pending) {
            try {
                remoteClient.send(entry);
                syncQueue.markProcessed(entry.id());
                succeeded++;
            } catch (Exception e) {
                syncQueue.markFailed(entry.id(), e.getMessage());
                failed++;
            }
        }

        return new SyncReplayResult(succeeded, failed, null);
    }

    public record SyncReplayResult(int succeeded, int failed, String error) {
    }
}