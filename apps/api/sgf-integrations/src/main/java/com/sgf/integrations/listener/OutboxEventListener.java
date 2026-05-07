package com.sgf.integrations.listener;

import com.sgf.core.event.DomainEvent;
import com.sgf.core.event.ProductCreatedEvent;
import com.sgf.core.event.SaleCompletedEvent;
import com.sgf.core.event.StockUpdatedEvent;
import com.sgf.integrations.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to domain events and enqueues them in the outbox for external integration.
 */
@Component
public class OutboxEventListener {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventListener.class);
    private final OutboxService outboxService;

    public OutboxEventListener(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @EventListener
    public void onSaleCompleted(SaleCompletedEvent event) {
        log.debug("Outbox: Sale completed {}, enqueuing for integration", event.saleId());
        outboxService.enqueue(
            event.aggregateType(),
            event.aggregateId(),
            event.eventType(),
            event.payload()
        );
    }

    @EventListener
    public void onStockUpdated(StockUpdatedEvent event) {
        // We only enqueue stock updates for specific reasons (e.g., RECEIPT or critical adjustments)
        if ("RECEIPT".equals(event.reason()) || event.quantityChange() < -100) {
            log.debug("Outbox: Critical stock update for product {}, enqueuing", event.productId());
            outboxService.enqueue(
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.payload()
            );
        }
    }

    @EventListener
    public void onProductCreated(ProductCreatedEvent event) {
        log.debug("Outbox: Product created {}, enqueuing", event.productId());
        outboxService.enqueue(
            event.aggregateType(),
            event.aggregateId(),
            event.eventType(),
            event.payload()
        );
    }
}
