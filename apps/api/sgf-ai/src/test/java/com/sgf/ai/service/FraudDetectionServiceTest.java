package com.sgf.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock private AnomalyDetector anomalyDetector;

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        fraudDetectionService = new FraudDetectionService(anomalyDetector);
    }

    @Test
    void shouldReturnHighRiskAnalysisForLargeAmount() {
        UUID saleId = UUID.randomUUID();
        when(anomalyDetector.computeAnomalyScore(eqDouble(5000.0), anyList())).thenReturn(0.91);
        when(anomalyDetector.classify(0.91)).thenReturn("CRITICAL");

        FraudDetectionService.FraudAnalysis analysis = fraudDetectionService.analyzeSale(saleId, "AFF-1", 5000.0);

        assertEquals(saleId, analysis.saleId());
        assertEquals("CRITICAL", analysis.riskLevel());
        assertTrue(analysis.isHighRisk());
        assertTrue(analysis.isCritical());
        assertTrue(analysis.reason().contains("HIGH_AMOUNT_ANOMALY"));
    }

    @Test
    void shouldBoostRiskWhenCustomerIdIsMissing() {
        UUID saleId = UUID.randomUUID();
        when(anomalyDetector.computeAnomalyScore(eqDouble(900.0), anyList())).thenReturn(0.10);
        when(anomalyDetector.classify(0.10)).thenReturn("NORMAL");

        FraudDetectionService.FraudAnalysis analysis = fraudDetectionService.analyzeSale(saleId, "", 900.0);

        assertFalse(analysis.reason().isBlank());
        assertTrue(analysis.reason().contains("MISSING_CUSTOMER_ID"));
        assertTrue(analysis.anomalyScore() >= 0.30);
    }

    @Test
    void shouldReturnNormalReasonWhenNoAnomaly() {
        UUID saleId = UUID.randomUUID();
        when(anomalyDetector.computeAnomalyScore(eqDouble(600.0), anyList())).thenReturn(0.15);
        when(anomalyDetector.classify(0.15)).thenReturn("NORMAL");

        FraudDetectionService.FraudAnalysis analysis = fraudDetectionService.analyzeSale(saleId, "AFF-1", 600.0);

        assertEquals("NORMAL", analysis.reason());
        assertFalse(analysis.isHighRisk());
    }

    private static double eqDouble(double value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @SuppressWarnings("unchecked")
    private static List<Double> anyList() {
        return org.mockito.ArgumentMatchers.anyList();
    }
}
