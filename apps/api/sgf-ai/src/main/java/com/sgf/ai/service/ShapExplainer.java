package com.sgf.ai.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides basic SHAP (SHapley Additive exPlanations)-inspired feature importance
 * for AI model outputs. Helps pharmacists understand WHY a forecast was made.
 */
@Service
public class ShapExplainer {

    private static final Logger log = LoggerFactory.getLogger(ShapExplainer.class);

    /**
     * Explain a demand forecast result.
     * Returns a map of feature name → contribution score.
     */
    public Map<String, Double> explainForecast(UUID productId, int predictedUnits, String model) {
        log.debug("Generating SHAP explanation for product {} forecast", productId);

        // In production: call ONNX model with SHAP integration or use TreeExplainer
        // Here we provide a rule-based approximation for transparency
        Map<String, Double> contributions = new LinkedHashMap<>();

        // Simulated SHAP values (would come from model internals in production)
        contributions.put("historical_avg_demand", 0.42);
        contributions.put("seasonal_trend", 0.23);
        contributions.put("days_to_expiry_stock", -0.08);
        contributions.put("price_elasticity", 0.15);
        contributions.put("obra_social_coverage", 0.12);
        contributions.put("competitor_proximity", -0.06);

        // Normalize so absolute values sum to ~1
        double total = contributions.values().stream().mapToDouble(Math::abs).sum();
        contributions.replaceAll((k, v) -> Math.round((v / total) * 1000.0) / 1000.0);

        return contributions;
    }

    /**
     * Explain a fraud detection result.
     */
    public Map<String, Double> explainFraud(UUID saleId, double anomalyScore) {
        Map<String, Double> contributions = new LinkedHashMap<>();
        contributions.put("transaction_amount", anomalyScore > 0.65 ? 0.55 : 0.05);
        contributions.put("customer_frequency", 0.20);
        contributions.put("time_of_day", 0.08);
        contributions.put("prescription_age_days", 0.12);
        contributions.put("affiliate_history", anomalyScore > 0.40 ? -0.15 : 0.05);
        return contributions;
    }
}
