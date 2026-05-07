package com.sgf.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgf.catalog.domain.Product;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpiryAlertServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ExpiryAlertService.AlertDispatcher alertDispatcher;

    @InjectMocks
    private ExpiryAlertService expiryAlertService;

    private UUID batchId;
    private UUID productId;
    private Batch expiringBatch;

    @BeforeEach
    void setUp() {
        batchId = UUID.randomUUID();
        productId = UUID.randomUUID();
        
        Product product = new Product();
        product.setId(productId);
        product.setCommercialName("IBUPROFENO 600MG");
        
        expiringBatch = new Batch();
        expiringBatch.setId(batchId);
        expiringBatch.setProduct(product);
        expiringBatch.setLotNumber("LOT-EXP-2024");
        expiringBatch.setExpiresAt(LocalDate.now().plusDays(25));
        expiringBatch.setAvailableQuantity(50);
    }

    @Test
    void getExpiryAlerts_ReturnsAlertsForExpiringBatches() {
        // Given
        when(batchRepository.findByExpiresAtBetweenAndAvailableQuantityGreaterThan(
                any(LocalDate.class), any(LocalDate.class), eq(0)))
            .thenReturn(Collections.singletonList(expiringBatch));

        // When
        List<ExpiryAlertService.ExpiryAlert> alerts = expiryAlertService.getExpiryAlerts(30);

        // Then
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        ExpiryAlertService.ExpiryAlert alert = alerts.get(0);
        assertEquals(batchId, alert.batchId());
        assertEquals(productId, alert.productId());
        assertEquals("IBUPROFENO 600MG", alert.productName());
        assertEquals("LOT-EXP-2024", alert.lotNumber());
        assertEquals(50, alert.availableQuantity());
        assertTrue(alert.daysUntilExpiry() <= 30);
        assertEquals(ExpiryAlertService.ExpirySeverity.CRITICAL, alert.severity());
    }

    @Test
    void getExpiryAlerts_EmptyList_WhenNoExpiringBatches() {
        // Given
        when(batchRepository.findByExpiresAtBetweenAndAvailableQuantityGreaterThan(
                any(LocalDate.class), any(LocalDate.class), eq(0)))
            .thenReturn(Collections.emptyList());

        // When
        List<ExpiryAlertService.ExpiryAlert> alerts = expiryAlertService.getExpiryAlerts(30);

        // Then
        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void getExpiryAlerts_CorrectSeverityClassification() {
        // Given batches at different expiry windows
        Product product = new Product();
        product.setId(productId);
        product.setCommercialName("ASPIRINA 100MG");

        Batch warningBatch = new Batch();
        warningBatch.setId(UUID.randomUUID());
        warningBatch.setProduct(product);
        warningBatch.setLotNumber("LOT-WARNING");
        warningBatch.setExpiresAt(LocalDate.now().plusDays(75));
        warningBatch.setAvailableQuantity(100);

        Batch actionBatch = new Batch();
        actionBatch.setId(UUID.randomUUID());
        actionBatch.setProduct(product);
        actionBatch.setLotNumber("LOT-ACTION");
        actionBatch.setExpiresAt(LocalDate.now().plusDays(45));
        actionBatch.setAvailableQuantity(80);

        when(batchRepository.findByExpiresAtBetweenAndAvailableQuantityGreaterThan(
                any(LocalDate.class), any(LocalDate.class), eq(0)))
            .thenReturn(Arrays.asList(warningBatch, actionBatch, expiringBatch));

        // When
        List<ExpiryAlertService.ExpiryAlert> alerts = expiryAlertService.getExpiryAlerts(90);

        // Then
        assertEquals(3, alerts.size());
        
        ExpiryAlertService.ExpiryAlert criticalAlert = alerts.stream()
            .filter(a -> a.lotNumber().equals("LOT-EXP-2024"))
            .findFirst()
            .orElseThrow();
        assertEquals(ExpiryAlertService.ExpirySeverity.CRITICAL, criticalAlert.severity());

        ExpiryAlertService.ExpiryAlert actionAlert = alerts.stream()
            .filter(a -> a.lotNumber().equals("LOT-ACTION"))
            .findFirst()
            .orElseThrow();
        assertEquals(ExpiryAlertService.ExpirySeverity.ACTION, actionAlert.severity());

        ExpiryAlertService.ExpiryAlert warningAlert = alerts.stream()
            .filter(a -> a.lotNumber().equals("LOT-WARNING"))
            .findFirst()
            .orElseThrow();
        assertEquals(ExpiryAlertService.ExpirySeverity.WARNING, warningAlert.severity());
    }

    @Test
    void checkExpiries_DispatchesAlertsForAllSeverityLevels() {
        // Given
        when(batchRepository.findByExpiresAtBetweenAndAvailableQuantityGreaterThan(
                any(LocalDate.class), any(LocalDate.class), eq(0)))
            .thenReturn(Collections.singletonList(expiringBatch));

        // When
        expiryAlertService.checkExpiries();

        // Then
        verify(alertDispatcher, atLeastOnce()).dispatch(any(ExpiryAlertService.ExpiryAlert.class));
    }

    @Test
    void loggingAlertDispatcher_DispatchesToLog() {
        // Given
        ExpiryAlertService.LoggingAlertDispatcher dispatcher = new ExpiryAlertService.LoggingAlertDispatcher();
        ExpiryAlertService.ExpiryAlert alert = new ExpiryAlertService.ExpiryAlert(
            batchId, productId, "TEST PRODUCT", "LOT-123",
            LocalDate.now().plusDays(15), 100, 15,
            ExpiryAlertService.ExpirySeverity.CRITICAL
        );

        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> dispatcher.dispatch(alert));
    }
}
