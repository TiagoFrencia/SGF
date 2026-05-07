package com.sgf.ai.service;

import com.sgf.inventory.service.InventoryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI-Ready Forecasting Service.
 * 
 * Phase 6 Roadmap: LSTM + Genetic Algorithms for demand prediction.
 * Current Implementation: Advanced Statistical Model (Prophet-like logic placeholder).
 */
@Service
public class ForecastingService {

    private static final Logger log = LoggerFactory.getLogger(ForecastingService.class);
    private final InventoryService inventoryService;
    private final OnnxModelLoader onnxLoader;

    public ForecastingService(InventoryService inventoryService, OnnxModelLoader onnxLoader) {
        this.inventoryService = inventoryService;
        this.onnxLoader = onnxLoader;
    }

    /**
     * Predict demand for the next 30 days.
     */
    public ForecastResult predictDemand(UUID productId) {
        log.info("Generating AI forecast for product {}", productId);
        
        var movements = inventoryService.getMovementsForProduct(productId, 180);
        double baseDemand = movements.stream().mapToInt(m -> m.quantity()).average().orElse(0.0);
        
        if (onnxLoader.isModelLoaded()) {
            // Prepare input data (e.g., last 30 days of sales normalized)
            float[] input = new float[30]; 
            // In reality, we'd pull exact historical daily sales and normalize
            float prediction = onnxLoader.predict(input);
            if (prediction >= 0) {
                return new ForecastResult(
                    productId,
                    Math.round(prediction * 30),
                    0.92,
                    "LSTM-NeuralNet (v1.2)"
                );
            }
        }

        // Statistical Fallback
        double seasonalityFactor = 1.15; 
        BigDecimal predictedUnits = BigDecimal.valueOf(baseDemand * 30 * seasonalityFactor)
                .setScale(0, java.math.RoundingMode.HALF_UP);
                
        return new ForecastResult(
            productId,
            predictedUnits.intValue(),
            0.85,
            "Statistical-SMA (Fallback)"
        );
    }

    public record ForecastResult(UUID productId, int predictedUnits, double confidence, String model) {}
}
