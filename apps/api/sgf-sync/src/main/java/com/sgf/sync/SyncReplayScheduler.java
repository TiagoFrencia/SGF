package com.sgf.sync;

import com.sgf.sync.queue.SyncReplayProcessor;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs background sync replay on a fixed interval once sync runtime beans are active.
 */
public class SyncReplayScheduler {

    private final SyncReplayProcessor syncReplayProcessor;

    public SyncReplayScheduler(SyncReplayProcessor syncReplayProcessor) {
        this.syncReplayProcessor = syncReplayProcessor;
    }

    @Scheduled(fixedDelay = 30_000)
    public void scheduledReplay() {
        syncReplayProcessor.replay();
    }
}
