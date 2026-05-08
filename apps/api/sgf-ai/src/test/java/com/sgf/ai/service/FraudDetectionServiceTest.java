package com.sgf.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para FraudDetectionService
 * Valida la detección de transacciones fraudulentas
 */
@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private AnomalyDetector anomalyDetector;

    @Mock
    private ShapExplainer shapExplainer;

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        fraudDetectionService = new FraudDetectionService(anomalyDetector, shapExplainer);
    }

    @Test
    void shouldDetectFraud_WhenAnomalyScoreExceedsThreshold() {
        // Given
        Map<String, Object> transaction = Map.of(
            "amount", 5000.0,
            "productId", "PROD-001",
            "quantity", 100,
            "branchId", "BRANCH-001"
        );
        
        when(anomalyDetector.detectAnomaly(any())).thenReturn(0.95); // Score alto

        // When
        boolean isFraud = fraudDetectionService.isFraudulent(transaction);

        // Then
        assertTrue(isFraud);
    }

    @Test
    void shouldNotDetectFraud_WhenAnomalyScoreBelowThreshold() {
        // Given
        Map<String, Object> transaction = Map.of(
            "amount", 100.0,
            "productId", "PROD-002",
            "quantity", 2,
            "branchId", "BRANCH-001"
        );
        
        when(anomalyDetector.detectAnomaly(any())).thenReturn(0.15); // Score bajo

        // When
        boolean isFraud = fraudDetectionService.isFraudulent(transaction);

        // Then
        assertFalse(isFraud);
    }

    @Test
    void shouldApplyBusinessRules_BeforeAnomalyDetection() {
        // Given
        // Transacción con cantidad inusualmente alta (regla de negocio)
        Map<String, Object> suspiciousTransaction = Map.of(
            "amount", 1000.0,
            "productId", "MED-CONTROLLED",
            "quantity", 500, // Cantidad sospechosa
            "branchId", "BRANCH-001"
        );

        when(anomalyDetector.detectAnomaly(any())).thenReturn(0.75);

        // When
        boolean isFraud = fraudDetectionService.isFraudulent(suspiciousTransaction);

        // Then
        assertTrue(isFraud);
    }

    @Test
    void shouldProvideFraudExplanation_WhenFraudDetected() {
        // Given
        Map<String, Object> transaction = Map.of(
            "amount", 5000.0,
            "productId", "PROD-001",
            "quantity", 100
        );
        
        when(anomalyDetector.detectAnomaly(any())).thenReturn(0.90);
        when(shapExplainer.explainPrediction(any())).thenReturn(
            Map.of("amount", 0.6, "quantity", 0.3, "productId", 0.1)
        );

        // When
        Map<String, Double> explanation = fraudDetectionService.explainFraudDetection(transaction);

        // Then
        assertNotNull(explanation);
        assertFalse(explanation.isEmpty());
        assertTrue(explanation.containsKey("amount"));
    }

    @Test
    void shouldFlagHighValueTransactions() {
        // Given
        double amount = 10000.0;
        fraudDetectionService.setHighValueThreshold(5000.0);

        // When
        boolean isHighValue = fraudDetectionService.isHighValueTransaction(amount);

        // Then
        assertTrue(isHighValue);
    }

    @Test
    void shouldDetectUnusualTimePattern() {
        // Given
        String hour = "03:00"; // 3 AM - hora inusual para farmacia
        
        // When
        boolean isUnusualHour = fraudDetectionService.isUnusualTransactionHour(hour);

        // Then
        assertTrue(isUnusualHour);
    }

    @Test
    void shouldCalculateRiskScore() {
        // Given
        Map<String, Object> factors = Map.of(
            "amountRisk", 0.8,
            "quantityRisk", 0.6,
            "timeRisk", 0.9,
            "productRisk", 0.5
        );

        // When
        double riskScore = fraudDetectionService.calculateRiskScore(factors);

        // Then
        assertNotNull(riskScore);
        assertTrue(riskScore >= 0.0 && riskScore <= 1.0);
    }

    @Test
    void shouldGenerateFraudAlert() {
        // Given
        Map<String, Object> transaction = Map.of(
            "id", "TXN-001",
            "amount", 5000.0,
            "branchId", "BRANCH-001"
        );
        double confidence = 0.92;

        // When
        Optional<Map<String, Object>> alert = fraudDetectionService.generateFraudAlert(transaction, confidence);

        // Then
        assertTrue(alert.isPresent());
        assertEquals("HIGH_RISK", alert.get().get("severity"));
    }

    @Test
    void shouldBatchProcessTransactions() {
        // Given
        List<Map<String, Object>> transactions = List.of(
            Map.of("id", "TXN-001", "amount", 100.0),
            Map.of("id", "TXN-002", "amount", 5000.0),
            Map.of("id", "TXN-003", "amount", 150.0)
        );
        
        when(anomalyDetector.detectAnomaly(any())).thenReturn(0.2, 0.95, 0.3);

        // When
        List<Boolean> fraudResults = fraudDetectionService.batchDetect(transactions);

        // Then
        assertNotNull(fraudResults);
        assertEquals(3, fraudResults.size());
        assertFalse(fraudResults.get(0));
        assertTrue(fraudResults.get(1));
        assertFalse(fraudResults.get(2));
    }
}
