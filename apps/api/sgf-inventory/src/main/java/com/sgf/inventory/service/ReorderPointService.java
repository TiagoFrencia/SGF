package com.sgf.inventory.service;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates dynamic reorder points based on historical demand.
 * Uses a simple moving average (SMA) over configurable window (default 90 days).
 * <p>
 * Reorder Point = Avg Daily Demand × Lead Time Days + Safety Stock
 * Safety Stock = Z × σ_d × √(Lead Time)
 * <p>
 * Future: replace SMA with ML-based LSTM forecast from Phase 6.
 */
@Service
public class ReorderPointService {

    private static final Logger log = LoggerFactory.getLogger(ReorderPointService.class);

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final ReorderPointRepository reorderPointRepository;
    private final InventoryService inventoryService;
    private final ReorderAlertDispatcher alertDispatcher;

    // Configurable defaults
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;
    private static final int DEFAULT_ANALYSIS_WINDOW_DAYS = 90;
    private static final double Z_SCORE_95_PERCENT = 1.645; // 95% service level
    private static final int MIN_SAFETY_STOCK = 3;

    public ReorderPointService(BatchRepository batchRepository,
                               ProductRepository productRepository,
                               ReorderPointRepository reorderPointRepository,
                               InventoryService inventoryService,
                               ReorderAlertDispatcher alertDispatcher) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.reorderPointRepository = reorderPointRepository;
        this.inventoryService = inventoryService;
        this.alertDispatcher = alertDispatcher;
    }

    /**
     * Daily recalculation of reorder points based on latest demand.
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void recalculateAll() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            try {
                ReorderCalculation calc = calculate(product.getId(), DEFAULT_ANALYSIS_WINDOW_DAYS, DEFAULT_LEAD_TIME_DAYS);
                reorderPointRepository.save(calc);
            } catch (Exception e) {
                log.error("Reorder calc failed for product {}", product.getId(), e);
            }
        }
        log.info("Recalculated reorder points for {} products", products.size());
    }

    /**
     * Calculate reorder point for a specific product.
     */
    @Transactional(readOnly = true)
    public ReorderCalculation calculate(UUID productId, int analysisDays, int leadTimeDays) {
        Product product = productRepository.findById(productId).orElseThrow();
        DemandStats stats = computeDemandStats(productId, analysisDays);

        int currentStock = batchRepository.findByProductIdAndAvailableQuantityGreaterThan(productId, 0)
                .stream().mapToInt(Batch::getAvailableQuantity).sum();

        BigDecimal avgDailyDemand = stats.totalUnits() > 0
                ? BigDecimal.valueOf(stats.totalUnits()).divide(BigDecimal.valueOf(analysisDays), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Safety stock: Z × σ × √(lead_time)
        double safetyStock = Z_SCORE_95_PERCENT * stats.stdDev() * Math.sqrt(leadTimeDays);
        int safetyStockRounded = Math.max(MIN_SAFETY_STOCK, (int) Math.ceil(safetyStock));

        // Reorder point = (avg_daily_demand × lead_time) + safety_stock
        int reorderPoint = avgDailyDemand.multiply(BigDecimal.valueOf(leadTimeDays))
                .add(BigDecimal.valueOf(safetyStockRounded))
                .intValue();

        // Economic Order Quantity (EOQ) - simple version
        int eoq = Math.max(reorderPoint, (int) Math.ceil(reorderPoint * 2.5));

        boolean needsReorder = currentStock <= reorderPoint;

        return new ReorderCalculation(
                product.getId(),
                product.getCommercialName(),
                product.getGtin(),
                currentStock,
                avgDailyDemand,
                reorderPoint,
                eoq,
                safetyStockRounded,
                leadTimeDays,
                analysisDays,
                stats.totalUnits(),
                stats.stdDev(),
                needsReorder,
                LocalDate.now()
        );
    }

    /**
     * Check all products and dispatch reorder alerts for those below threshold.
     */
    @Scheduled(cron = "0 30 7 * * *")
    @Transactional(readOnly = true)
    public void checkReorderAlerts() {
        List<Product> products = productRepository.findAll();
        int alerts = 0;
        for (Product product : products) {
            ReorderCalculation calc = calculate(product.getId(), DEFAULT_ANALYSIS_WINDOW_DAYS, DEFAULT_LEAD_TIME_DAYS);
            if (calc.needsReorder()) {
                alertDispatcher.dispatch(calc);
                alerts++;
            }
        }
        if (alerts > 0) {
            log.warn("{} products need reorder", alerts);
        }
    }

    private DemandStats computeDemandStats(UUID productId, int windowDays) {
        // In a full implementation, this queries sale_items joined with sales
        // for completed sales in the analysis window.
        // For now it uses a placeholder that would read from analytics_daily_sales view.
        // The actual implementation queries StockMovementRepository for OUT movements.
        try {
            var movements = inventoryService.getMovementsForProduct(productId, windowDays);
            if (movements.isEmpty()) {
                return new DemandStats(0, 0.0);
            }
            int totalUnits = movements.stream().mapToInt(m -> m.quantity()).sum();
            double mean = (double) totalUnits / windowDays;
            double variance = movements.stream()
                    .mapToDouble(m -> Math.pow(m.quantity() - mean, 2))
                    .average()
                    .orElse(0.0);
            return new DemandStats(totalUnits, Math.sqrt(variance));
        } catch (Exception e) {
            log.warn("Demand stats unavailable for product {}: {}", productId, e.getMessage());
            return new DemandStats(0, 0.0);
        }
    }

    public record DemandStats(int totalUnits, double stdDev) {
    }

    public record ReorderCalculation(
            UUID productId,
            String productName,
            String gtin,
            int currentStock,
            BigDecimal avgDailyDemand,
            int reorderPoint,
            int eoq,
            int safetyStock,
            int leadTimeDays,
            int analysisWindowDays,
            int totalDemandInWindow,
            double demandStdDev,
            boolean needsReorder,
            LocalDate calculatedAt
    ) {
    }

    public interface ReorderAlertDispatcher {
        void dispatch(ReorderCalculation calculation);
    }

    @org.springframework.stereotype.Component
    public static class LoggingReorderAlertDispatcher implements ReorderAlertDispatcher {
        @Override
        public void dispatch(ReorderCalculation calc) {
            log.warn("[REORDER_NEEDED] product={} stock={} reorderPoint={} eoq={} avgDaily={}",
                    calc.productName(), calc.currentStock(), calc.reorderPoint(),
                    calc.eoq(), calc.avgDailyDemand());
        }
    }

    /**
     * Simple in-memory repository for reorder calculations.
     * In production this maps to a DB table for historical tracking.
     */
    public interface ReorderPointRepository {
        void save(ReorderCalculation calculation);
    }
}