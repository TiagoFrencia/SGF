package com.sgf.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple in-memory event bus for CQRS.
 * In production this would be Kafka/RabbitMQ.
 */
public class EventBus {

    private static final EventBus INSTANCE = new EventBus();
    private final List<EventListener> listeners = Collections.synchronizedList(new ArrayList<>());

    private EventBus() {
    }

    public static EventBus instance() {
        return INSTANCE;
    }

    public void register(EventListener listener) {
        listeners.add(listener);
    }

    public void publish(DomainEvent event) {
        listeners.forEach(listener -> {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // In production: dead-letter queue
                System.err.println("EventBus error: " + e.getMessage());
            }
        });
    }

    @FunctionalInterface
    public interface EventListener {
        void onEvent(DomainEvent event);
    }
}