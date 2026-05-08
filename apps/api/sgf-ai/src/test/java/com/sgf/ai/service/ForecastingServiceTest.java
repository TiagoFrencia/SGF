package com.sgf.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para ForecastingService
 * Valida las predicciones de demanda de productos
 */
@ExtendWith(MockitoExtension.class)
class ForecastingServiceTest {

    @Mock
    private OnnxModelLoader modelLoader;

    private ForecastingService forecastingService;

    @BeforeEach
    void setUp() {
        forecastingService = new ForecastingService(modelLoader);
    }

    @Test
    void shouldPredictDemand_WhenHistoricalDataProvided() {
        // Given
        String productId = "PROD-001";
        List<Double> historicalSales = List.of(10.0, 15.0, 12.0, 18.0, 20.0, 25.0, 22.0, 28.0);
        
        when(modelLoader.predict(any(), any())).thenReturn(30.5);

        // When
        Double prediction = forecastingService.predictDemand(productId, historicalSales, 7);

        // Then
        assertNotNull(prediction);
        assertTrue(prediction > 0);
        assertEquals(30.5, prediction);
    }

    @Test
    void shouldReturnEmptyPrediction_WhenNoHistoricalData() {
        // Given
        String productId = "PROD-002";
        List<Double> historicalSales = List.of();

        // When
        Double prediction = forecastingService.predictDemand(productId, historicalSales, 7);

        // Then
        assertNull(prediction);
    }

    @Test
    void shouldCalculateReorderPoint_WhenDemandAndLeadTimeProvided() {
        // Given
        double dailyDemand = 25.0;
        int leadTimeDays = 14;
        double safetyStock = 50.0;

        // When
        double reorderPoint = forecastingService.calculateReorderPoint(dailyDemand, leadTimeDays, safetyStock);

        // Then
        assertEquals(400.0, reorderPoint); // 25 * 14 + 50
    }

    @Test
    void shouldGenerateForecastForMultipleProducts() {
        // Given
        Map<String, List<Double>> productsHistory = Map.of(
            "PROD-001", List.of(10.0, 15.0, 12.0),
            "PROD-002", List.of(20.0, 25.0, 22.0),
            "PROD-003", List.of(5.0, 8.0, 6.0)
        );
        
        when(modelLoader.predict(any(), any())).thenReturn(20.0);

        // When
        Map<String, Double> forecasts = forecastingService.generateBatchForecast(productsHistory, 7);

        // Then
        assertNotNull(forecasts);
        assertEquals(3, forecasts.size());
        assertTrue(forecasts.values().stream().allMatch(v -> v == 20.0));
    }

    @Test
    void shouldDetectSeasonalPattern_WhenEnoughData() {
        // Given
        // Datos con patrón semanal (ventas más altas los viernes)
        List<Double> weeklyPattern = List.of(
            10.0, 12.0, 11.0, 13.0, 20.0, 18.0, 15.0,  // Semana 1
            11.0, 13.0, 12.0, 14.0, 22.0, 19.0, 16.0   // Semana 2
        );

        // When
        boolean hasSeasonality = forecastingService.detectSeasonality(weeklyPattern, 7);

        // Then
        assertTrue(hasSeasonality);
    }

    @Test
    void shouldThrowException_WhenInsufficientDataForSeasonality() {
        // Given
        List<Double> insufficientData = List.of(1.0, 2.0);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            forecastingService.detectSeasonality(insufficientData, 7)
        );
    }

    @Test
    void shouldCalculateMovingAverage() {
        // Given
        List<Double> values = List.of(10.0, 15.0, 20.0, 25.0, 30.0);
        int windowSize = 3;

        // When
        double movingAvg = forecastingService.calculateMovingAverage(values, windowSize);

        // Then
        assertEquals(25.0, movingAvg); // Promedio de últimos 3: (20+25+30)/3
    }

    @Test
    void shouldProvideConfidenceInterval() {
        // Given
        double prediction = 100.0;
        double stdDeviation = 10.0;
        int confidenceLevel = 95;

        // When
        Map<String, Double> interval = forecastingService.getConfidenceInterval(prediction, stdDeviation, confidenceLevel);

        // Then
        assertNotNull(interval);
        assertTrue(interval.containsKey("lower"));
        assertTrue(interval.containsKey("upper"));
        assertTrue(interval.get("lower") < prediction);
        assertTrue(interval.get("upper") > prediction);
    }
}
