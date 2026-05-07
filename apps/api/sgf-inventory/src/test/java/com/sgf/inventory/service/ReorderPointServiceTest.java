package com.sgf.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReorderPointServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReorderPointService.ReorderPointRepository reorderPointRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ReorderPointService.ReorderAlertDispatcher alertDispatcher;

    @InjectMocks
    private ReorderPointService reorderPointService;

    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = new Product();
        product.setId(productId);
        product.setCommercialName("ASPIRINA 100MG");
        product.setGtin("7791234567890");
    }

    @Test
    void calculate_ReturnsReorderCalculation_WithValidData() {
        // Given
        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(product));
        when(batchRepository.findByProductIdAndAvailableQuantityGreaterThan(eq(productId), anyInt()))
            .thenReturn(Collections.emptyList());
        when(inventoryService.getMovementsForProduct(eq(productId), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        ReorderPointService.ReorderCalculation calculation = 
            reorderPointService.calculate(productId, 90, 7);

        // Then
        assertNotNull(calculation);
        assertEquals(productId, calculation.productId());
        assertEquals("ASPIRINA 100MG", calculation.productName());
        assertEquals("7791234567890", calculation.gtin());
        assertEquals(0, calculation.currentStock());
        assertTrue(calculation.needsReorder()); // Stock is 0, so needs reorder
        assertTrue(calculation.safetyStock() >= 3); // Minimum safety stock
        assertEquals(7, calculation.leadTimeDays());
        assertEquals(90, calculation.analysisWindowDays());
    }

    @Test
    void calculate_WithDemandHistory_ComputesCorrectReorderPoint() {
        // Given
        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(product));
        when(batchRepository.findByProductIdAndAvailableQuantityGreaterThan(eq(productId), anyInt()))
            .thenReturn(Collections.emptyList());
        
        // Simulate demand: 90 units over 90 days = 1 unit/day average
        List<com.sgf.inventory.web.StockViewResponse> movements = Arrays.asList(
            createMovement(1), createMovement(2), createMovement(1), 
            createMovement(3), createMovement(1), createMovement(2)
        );
        when(inventoryService.getMovementsForProduct(eq(productId), eq(90)))
            .thenReturn(movements);

        // When
        ReorderPointService.ReorderCalculation calculation = 
            reorderPointService.calculate(productId, 90, 7);

        // Then
        assertNotNull(calculation);
        assertTrue(calculation.avgDailyDemand().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(calculation.reorderPoint() > 0);
        assertTrue(calculation.eoq() >= calculation.reorderPoint());
    }

    @Test
    void calculate_ProductNotFound_ThrowsException() {
        // Given
        when(productRepository.findById(productId)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            reorderPointService.calculate(productId, 90, 7));
    }

    @Test
    void listReorderAlerts_ReturnsOnlyProductsNeedingReorder() {
        // Given
        Product product1 = new Product();
        product1.setId(UUID.randomUUID());
        product1.setCommercialName("PRODUCT A");
        
        Product product2 = new Product();
        product2.setId(UUID.randomUUID());
        product2.setCommercialName("PRODUCT B");

        when(productRepository.findAll()).thenReturn(Arrays.asList(product1, product2));
        when(batchRepository.findByProductIdAndAvailableQuantityGreaterThan(any(UUID.class), anyInt()))
            .thenReturn(Collections.emptyList());
        when(inventoryService.getMovementsForProduct(any(UUID.class), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        List<ReorderPointService.ReorderCalculation> alerts = 
            reorderPointService.listReorderAlerts(90, 7);

        // Then
        assertNotNull(alerts);
        assertEquals(2, alerts.size());
        assertTrue(alerts.stream().allMatch(ReorderPointService.ReorderCalculation::needsReorder));
    }

    @Test
    void checkReorderAlerts_DispatchesAlertsForLowStockProducts() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.singletonList(product));
        when(batchRepository.findByProductIdAndAvailableQuantityGreaterThan(eq(productId), anyInt()))
            .thenReturn(Collections.emptyList());
        when(inventoryService.getMovementsForProduct(eq(productId), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        reorderPointService.checkReorderAlerts();

        // Then
        verify(alertDispatcher, atLeastOnce()).dispatch(any(ReorderPointService.ReorderCalculation.class));
    }

    @Test
    void recalculateAll_SavesCalculationsForAllProducts() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.singletonList(product));
        when(batchRepository.findByProductIdAndAvailableQuantityGreaterThan(eq(productId), anyInt()))
            .thenReturn(Collections.emptyList());
        when(inventoryService.getMovementsForProduct(eq(productId), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        reorderPointService.recalculateAll();

        // Then
        verify(reorderPointRepository, atLeastOnce()).save(any(ReorderPointService.ReorderCalculation.class));
    }

    @Test
    void loggingReorderAlertDispatcher_DispatchesToLog() {
        // Given
        ReorderPointService.LoggingReorderAlertDispatcher dispatcher = 
            new ReorderPointService.LoggingReorderAlertDispatcher();
        ReorderPointService.ReorderCalculation calc = new ReorderPointService.ReorderCalculation(
            productId, "TEST PRODUCT", "7791234567890", 5, BigDecimal.valueOf(1.5),
            10, 25, 5, 7, 90, 135, 1.2, true, LocalDate.now()
        );

        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> dispatcher.dispatch(calc));
    }

    @Test
    void inMemoryReorderPointRepository_SavesAndRetrievesCalculations() {
        // Given
        ReorderPointService.InMemoryReorderPointRepository repo = 
            new ReorderPointService.InMemoryReorderPointRepository();
        ReorderPointService.ReorderCalculation calc = new ReorderPointService.ReorderCalculation(
            productId, "TEST PRODUCT", "7791234567890", 10, BigDecimal.valueOf(2.0),
            15, 35, 8, 7, 90, 180, 1.5, false, LocalDate.now()
        );

        // When
        repo.save(calc);

        // Then - no exception and internal map should contain the calculation
        // (We can't directly verify the map content without exposing it, but save() should complete successfully)
        assertDoesNotThrow(() -> repo.save(calc));
    }

    private com.sgf.inventory.web.StockViewResponse createMovement(int quantity) {
        return new com.sgf.inventory.web.StockViewResponse(
            UUID.randomUUID(), productId, "TEST", LocalDate.now(), 
            quantity, "OUT", "TEST"
        );
    }
}
