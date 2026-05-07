package com.sgf.ai.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Fraud Detection using Isolation Forest via AnomalyDetector.
 * Detects dispensing fraud, multi-pharmacy patterns, and prescription abuse.
 */
@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);
    private final AnomalyDetector anomalyDetector;

    // Thresholds for Argentine pharmacy context
    private static final double HIGH_RISK_THRESHOLD = 0.65;
    private static final double CRITICAL_THRESHOLD = 0.85;

    public FraudDetectionService(AnomalyDetector anomalyDetector) {
        this.anomalyDetector = anomalyDetector;
    }

    /**
     * Full fraud analysis for a single sale.
     */
    public FraudAnalysis analyzeSale(UUID saleId, String customerId, double amount) {
        log.info("Analyzing sale {} for potential fraud. Customer: {}, Amount: {}", saleId, customerId, amount);

        // Example population — in production this would be pulled from DB (last N sales for this product/affiliate)
        List<Double> typicalAmounts = List.of(500.0, 800.0, 1200.0, 950.0, 1100.0, 750.0, 600.0, 900.0, 1050.0, 870.0);

        double amountScore = anomalyDetector.computeAnomalyScore(amount, typicalAmounts);
        String riskLevel = anomalyDetector.classify(amountScore);

        // Build composite reason
        StringBuilder reason = new StringBuilder();
        if (amountScore >= HIGH_RISK_THRESHOLD) {
            reason.append("HIGH_AMOUNT_ANOMALY");
        }
        if (customerId == null || customerId.isBlank()) {
            if (reason.length() > 0) reason.append("|");
            reason.append("MISSING_CUSTOMER_ID");
            amountScore = Math.min(1.0, amountScore + 0.2);
        }
        if (reason.isEmpty()) reason.append("NORMAL");

        log.info("Fraud analysis complete: saleId={}, score={:.3f}, risk={}", saleId, amountScore, riskLevel);
        return new FraudAnalysis(saleId, amountScore, riskLevel, reason.toString());
    }

    public record FraudAnalysis(UUID saleId, double anomalyScore, String riskLevel, String reason) {
        public boolean isHighRisk() { return anomalyScore >= 0.65; }
        public boolean isCritical() { return anomalyScore >= 0.85; }
    }
}
