package com.sgf.audit.listener;

import com.sgf.audit.service.AuditService;
import com.sgf.core.event.DomainEvent;
import com.sgf.core.event.MigrationFinishedEvent;
import com.sgf.core.event.MigrationStartedEvent;
import com.sgf.core.event.ProductCreatedEvent;
import com.sgf.core.event.SaleCompletedEvent;
import com.sgf.core.event.StockUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to domain events and records them in the audit log.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);
    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onProductCreated(ProductCreatedEvent event) {
        log.debug("Audit: Product created {}", event.productId());
        auditService.record(
            event.actorUsername(),
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event.payload(),
            event.tenantId()
        );
    }

    @EventListener
    public void onStockUpdated(StockUpdatedEvent event) {
        log.debug("Audit: Stock updated for product {}", event.productId());
        auditService.record(
            event.actorUsername(),
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event.payload(),
            event.tenantId()
        );
    }

    @EventListener
    public void onSaleCompleted(SaleCompletedEvent event) {
        log.debug("Audit: Sale completed {}", event.saleId());
        auditService.record(
            event.actorUsername(),
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event.payload(),
            event.tenantId()
        );
    }

    @EventListener
    public void onMigrationStarted(MigrationStartedEvent event) {
        log.info("Audit: Migration started {}", event.migrationId());
        auditService.record(
            "system",
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event.payload(),
            event.tenantId()
        );
    }

    @EventListener
    public void onMigrationFinished(MigrationFinishedEvent event) {
        log.info("Audit: Migration finished {}", event.migrationId());
        auditService.record(
            "system",
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event.payload(),
            event.tenantId()
        );
    }
}
