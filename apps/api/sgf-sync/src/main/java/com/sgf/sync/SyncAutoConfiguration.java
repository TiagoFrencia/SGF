package com.sgf.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.sync.local.LocalCommandHandler;
import com.sgf.sync.local.LocalDatabase;
import com.sgf.sync.local.LocalSyncQueue;
import com.sgf.sync.queue.RemoteSyncClient;
import com.sgf.sync.queue.SyncReplayProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "app.sync.enabled", havingValue = "true", matchIfMissing = true)
public class SyncAutoConfiguration {

    @Value("${app.sync.remote-url:http://localhost:8080}")
    private String remoteUrl;
    @Autowired
    private SyncReplayProcessor syncReplayProcessor;

    @Bean
    public LocalDatabase localDatabase() {
        LocalDatabase db = new LocalDatabase();
        db.initialize();
        return db;
    }

    @Bean
    public LocalSyncQueue localSyncQueue(LocalDatabase localDatabase, ObjectMapper objectMapper) {
        return new LocalSyncQueue(localDatabase, objectMapper);
    }

    @Bean
    public RemoteSyncClient remoteSyncClient() {
        return new RemoteSyncClient(remoteUrl);
    }

    @Bean
    public SyncReplayProcessor syncReplayProcessor(LocalSyncQueue localSyncQueue, RemoteSyncClient remoteSyncClient) {
        return new SyncReplayProcessor(localSyncQueue, remoteSyncClient);
    }

    @Bean
    public LocalCommandHandler localCommandHandler(LocalDatabase localDatabase, LocalSyncQueue localSyncQueue, ObjectMapper objectMapper) {
        return new LocalCommandHandler(localDatabase, localSyncQueue, objectMapper);
    }

    /**
     * Scheduled sync replay: attempts to push local events every 30 seconds.
     */
    @Scheduled(fixedDelay = 30_000)
    public void scheduledReplay() {
        syncReplayProcessor.replay();
    }
}