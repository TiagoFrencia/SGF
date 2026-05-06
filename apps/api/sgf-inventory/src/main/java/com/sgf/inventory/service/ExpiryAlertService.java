package com.sgf.inventory.service;

import com.sgf.catalog.domain.Product;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service that monitors batch expiry dates and generates alerts.
 * Runs daily at 8 AM.
 */
@Service
public class ExpiryAlertService {

    private static final Logger log = LoggerFactory.getLogger(ExpiryAlertService.class);

    private final BatchRepository batchRepository;
    private final AlertDispatcher alertDispatcher;

    public ExpiryAlertService(BatchRepository batchRepository, AlertDispatcher alertDispatcher) {
        this.batchRepository = batchRepository;
        this.alertDispatcher = alertDispatcher;
    }

    /**
     * Daily check for products approaching expiry.
     * Windows: 90 days (early warning), 60 days (action required), 30 days (critical).
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void checkExpiries() {
        LocalDate today = LocalDate.now();
        checkWindow(today, 90, ExpirySeverity.WARNING);
        checkWindow(today, 60, ExpirySeverity.ACTION);
        checkWindow(today, 30, ExpirySeverity.CRITICAL);
    }

    /**
     * Manual/on-demand expiry check. Returns alerts for the given window.
     */
    @Transactional(readOnly = true)
    public List<ExpiryAlert> getExpiryAlerts(int daysWindow) {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(daysWindow);
        List<Batch> expiring = batchRepository
                .findByExpiresAtBetweenAndAvailableQuantityGreaterThan(today, threshold, 0);

        return expiring.stream()
                .map(batch -> {
                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, batch.getExpiresAt());
                    ExpirySeverity severity = daysUntilExpiry <= 30 ? ExpirySeverity.CRITICAL
                            : daysUntilExpiry <= 60 ? ExpirySeverity.ACTION
                            : ExpirySeverity.WARNING;
                    return new ExpiryAlert(
                            batch.getId(),
                            batch.getProduct().getId(),
                            batch.getProduct().getCommercialName(),
                            batch.getLotNumber(),
                            batch.getExpiresAt(),
                            batch.getAvailableQuantity(),
                            daysUntilExpiry,
                            severity
                    );
                })
                .sorted((a, b) -> Long.compare(a.daysUntilExpiry(), b.daysUntilExpiry()))
                .toList();
    }

    private void checkWindow(LocalDate today, int days, ExpirySeverity severity) {
        LocalDate threshold = today.plusDays(days);
        List<Batch> expiring = batchRepository
                .findByExpiresAtBetweenAndAvailableQuantityGreaterThan(today, threshold, 0);

        if (!expiring.isEmpty()) {
            log.warn("{} batches with {} severity expiry within {} days", expiring.size(), severity, days);
            for (Batch batch : expiring) {
                alertDispatcher.dispatch(new ExpiryAlert(
                        batch.getId(),
                        batch.getProduct().getId(),
                        batch.getProduct().getCommercialName(),
                        batch.getLotNumber(),
                        batch.getExpiresAt(),
                        batch.getAvailableQuantity(),
                        java.time.temporal.ChronoUnit.DAYS.between(today, batch.getExpiresAt()),
                        severity
                ));
            }
        }
    }

    public enum ExpirySeverity {
        WARNING,    // 90 days
        ACTION,     // 60 days
        CRITICAL    // 30 days
    }

    public record ExpiryAlert(
            java.util.UUID batchId,
            java.util.UUID productId,
            String productName,
            String lotNumber,
            LocalDate expiresAt,
            int availableQuantity,
            long daysUntilExpiry,
            ExpirySeverity severity
    ) {
    }

    /**
     * Pluggable dispatcher: console, email, webhook, push notification, etc.
     */
    public interface AlertDispatcher {
        void dispatch(ExpiryAlert alert);
    }

    @org.springframework.stereotype.Component
    public static class LoggingAlertDispatcher implements AlertDispatcher {
        @Override
        public void dispatch(ExpiryAlert alert) {
            log.warn("[EXPIRY_ALERT] severity={} product={} lot={} expires={} qty={} daysLeft={}",
                    alert.severity(), alert.productName(), alert.lotNumber(),
                    alert.expiresAt(), alert.availableQuantity(), alert.daysUntilExpiry());
        }
    }
}