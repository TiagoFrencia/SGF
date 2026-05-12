package com.sgf.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.sync.local.LocalCommandHandler;
import com.sgf.sync.local.LocalDatabase;
import com.sgf.sync.local.LocalSyncQueue;
import com.sgf.sync.queue.RemoteSyncClient;
import com.sgf.sync.queue.SyncReplayProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "app.sync.enabled", havingValue = "true", matchIfMissing = true)
public class SyncAutoConfiguration {

    @Value("${app.sync.remote-url:http://localhost:8080}")
    private String remoteUrl;

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

    @Bean
    public SyncReplayScheduler syncReplayScheduler(SyncReplayProcessor syncReplayProcessor) {
        return new SyncReplayScheduler(syncReplayProcessor);
    }
}
