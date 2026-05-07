package com.sgf.sync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.sync.local.LastWriteWinsResolver;
import com.sgf.sync.local.LocalDatabase;
import com.sgf.sync.local.LocalSyncQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SyncConfig {

    @Bean(initMethod = "initialize")
    public LocalDatabase localDatabase() {
        return new LocalDatabase();
    }

    @Bean
    public LocalSyncQueue localSyncQueue(LocalDatabase localDatabase, ObjectMapper objectMapper) {
        return new LocalSyncQueue(localDatabase, objectMapper);
    }

    @Bean
    public LastWriteWinsResolver lastWriteWinsResolver() {
        return new LastWriteWinsResolver();
    }
}
