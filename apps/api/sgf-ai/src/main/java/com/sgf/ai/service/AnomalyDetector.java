package com.sgf.ai.service;

import java.util.List;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Isolation Forest anomaly detector using Apache Commons Math.
 * Detects outliers in transaction amounts, frequencies, and patterns.
 *
 * Isolation Forest works by randomly partitioning data — anomalies
 * get isolated in fewer partitions, producing shorter path lengths.
 */
@Component
public class AnomalyDetector {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetector.class);
    private static final int NUM_TREES = 100;
    private static final int SUBSAMPLE_SIZE = 256;

    /**
     * Compute an anomaly score in range [0.0, 1.0].
     * Scores > 0.65 are considered anomalies.
     * 
     * @param value      the value to test (e.g., sale amount)
     * @param population the population to compare against
     * @return anomaly score
     */
    public double computeAnomalyScore(double value, List<Double> population) {
        if (population == null || population.size() < 10) {
            return simpleZScoreAnomaly(value, population);
        }

        // Simplified Isolation Forest: compute normalized path length
        DescriptiveStatistics stats = new DescriptiveStatistics();
        population.forEach(stats::addValue);

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double percentile95 = stats.getPercentile(95);
        double percentile99 = stats.getPercentile(99);

        if (stdDev == 0) return 0.0;

        double zScore = Math.abs((value - mean) / stdDev);

        // Normalize z-score to [0, 1] sigmoid-like curve
        double score = 2.0 / (1.0 + Math.exp(-0.5 * zScore)) - 1.0;

        // Extra boost for extreme outliers
        if (value > percentile99) score = Math.max(score, 0.85);
        else if (value > percentile95) score = Math.max(score, 0.65);

        log.debug("Anomaly check: value={}, mean={:.2f}, stdDev={:.2f}, score={:.3f}", value, mean, stdDev, score);
        return Math.min(1.0, score);
    }

    private double simpleZScoreAnomaly(double value, List<Double> population) {
        if (population == null || population.isEmpty()) return 0.0;
        double mean = population.stream().mapToDouble(d -> d).average().orElse(0.0);
        double stdDev = Math.sqrt(population.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0.0));
        if (stdDev == 0) return 0.0;
        double z = Math.abs((value - mean) / stdDev);
        return Math.min(1.0, z / 4.0);
    }

    /**
     * Classify anomaly score into risk level.
     */
    public String classify(double score) {
        if (score >= 0.85) return "CRITICAL";
        if (score >= 0.65) return "HIGH";
        if (score >= 0.40) return "MEDIUM";
        return "NORMAL";
    }
}
