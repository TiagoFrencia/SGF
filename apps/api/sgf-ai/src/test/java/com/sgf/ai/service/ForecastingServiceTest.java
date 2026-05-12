package com.sgf.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sgf.inventory.service.InventoryService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForecastingServiceTest {

    @Mock private InventoryService inventoryService;
    @Mock private OnnxModelLoader modelLoader;

    private ForecastingService forecastingService;

    @BeforeEach
    void setUp() {
        forecastingService = new ForecastingService(inventoryService, modelLoader);
    }

    @Test
    void shouldUseOnnxPredictionWhenModelIsLoaded() {
        UUID productId = UUID.randomUUID();
        when(inventoryService.getMovementsForProduct(productId, 180)).thenReturn(List.of(
                new InventoryService.MovementSummary(OffsetDateTime.now().minusDays(2), 5),
                new InventoryService.MovementSummary(OffsetDateTime.now().minusDays(1), 7)
        ));
        when(modelLoader.isModelLoaded()).thenReturn(true);
        when(modelLoader.predict(org.mockito.ArgumentMatchers.any(float[].class))).thenReturn(12.4f);

        ForecastingService.ForecastResult result = forecastingService.predictDemand(productId);

        assertNotNull(result);
        assertEquals(productId, result.productId());
        assertEquals(372, result.predictedUnits());
        assertEquals("LSTM-NeuralNet (v1.2)", result.model());
    }

    @Test
    void shouldFallbackToStatisticalModelWhenOnnxUnavailable() {
        UUID productId = UUID.randomUUID();
        when(inventoryService.getMovementsForProduct(productId, 180)).thenReturn(List.of(
                new InventoryService.MovementSummary(OffsetDateTime.now().minusDays(2), 10),
                new InventoryService.MovementSummary(OffsetDateTime.now().minusDays(1), 20)
        ));
        when(modelLoader.isModelLoaded()).thenReturn(false);

        ForecastingService.ForecastResult result = forecastingService.predictDemand(productId);

        assertNotNull(result);
        assertEquals(productId, result.productId());
        assertTrue(result.predictedUnits() > 0);
        assertEquals("Statistical-SMA (Fallback)", result.model());
    }
}
