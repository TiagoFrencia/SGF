package com.sgf.sync.queue;

import com.sgf.sync.local.LocalSyncQueue.SyncEntry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client that sends local sync events to the remote SGF API.
 * <p>
 * In a full implementation, this would use JWT auth and batching.
 */
public class RemoteSyncClient {

    private final String remoteBaseUrl;
    private final HttpClient httpClient;

    public RemoteSyncClient(String remoteBaseUrl) {
        this.remoteBaseUrl = remoteBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isOnline() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(remoteBaseUrl + "/actuator/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public void send(SyncEntry entry) throws Exception {
        String body = String.format("""
                {
                  "aggregateType": "%s",
                  "aggregateId": "%s",
                  "eventType": "%s",
                  "payload": %s,
                  "originTimestamp": "%s"
                }
                """, entry.aggregateType(), entry.aggregateId(), entry.eventType(), entry.payload(), entry.createdAt());

        HttpRequest request = HttpRequest.newBuilder(URI.create(remoteBaseUrl + "/sync/events"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Remote sync rejected: HTTP " + response.statusCode() + " - " + response.body());
        }
    }
}