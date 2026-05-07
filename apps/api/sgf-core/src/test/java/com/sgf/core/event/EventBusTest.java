package com.sgf.core.event;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class EventBusTest {

    @Test
    void eventIsPublishedToListeners() {
        EventBus bus = EventBus.instance();
        AtomicBoolean received = new AtomicBoolean(false);

        bus.register(event -> received.set(true));
        bus.publish(new DomainEvent() {
            @Override public UUID aggregateId() { return UUID.randomUUID(); }
            @Override public String aggregateType() { return "TEST"; }
            @Override public String eventType() { return "TEST"; }
            @Override public String payload() { return "{}"; }
            @Override public OffsetDateTime occurredAt() { return OffsetDateTime.now(); }
        });

        assertTrue(received.get());
    }
}
