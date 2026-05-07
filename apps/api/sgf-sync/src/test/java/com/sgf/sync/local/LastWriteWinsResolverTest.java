package com.sgf.sync.local;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class LastWriteWinsResolverTest {

    private final LastWriteWinsResolver resolver = new LastWriteWinsResolver();

    @Test
    void newerLocalWins() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime older = now.minusMinutes(1);

        assertEquals("LOCAL", resolver.resolve("LOCAL", now, "REMOTE", older));
    }

    @Test
    void newerRemoteWins() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime older = now.minusMinutes(1);

        assertEquals("REMOTE", resolver.resolve("LOCAL", older, "REMOTE", now));
    }
}
