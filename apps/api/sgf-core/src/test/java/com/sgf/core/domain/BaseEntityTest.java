package com.sgf.core.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

    static class TestEntity extends BaseEntity {}

    @Test
    void prePersistGeneratesIdAndTimestamps() {
        TestEntity entity = new TestEntity();
        entity.prePersist();

        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertTrue(entity.getCreatedAt().isBefore(OffsetDateTime.now().plusSeconds(1)));
    }

    @Test
    void preUpdateUpdatesTimestamp() throws InterruptedException {
        TestEntity entity = new TestEntity();
        entity.prePersist();
        OffsetDateTime originalUpdate = entity.getUpdatedAt();

        Thread.sleep(10);
        entity.preUpdate();

        assertTrue(entity.getUpdatedAt().isAfter(originalUpdate));
    }
}
