package com.sgf.sync.local;

import java.time.OffsetDateTime;

/**
 * Resolver that implements Last-Write-Wins strategy for conflict resolution.
 */
public class LastWriteWinsResolver {

    /**
     * Resolves conflict between local and remote version.
     * @return "LOCAL" or "REMOTE"
     */
    public String resolve(String localValue, OffsetDateTime localTime, String remoteValue, OffsetDateTime remoteTime) {
        if (localTime == null) return "REMOTE";
        if (remoteTime == null) return "LOCAL";
        return localTime.isAfter(remoteTime) ? "LOCAL" : "REMOTE";
    }
}
