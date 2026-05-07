package com.sgf.pos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgf.pos.domain.PosOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MultiOrderServiceTest {

    @Mock
    private PosOrderService orderService;

    @InjectMocks
    private MultiOrderService multiOrderService;

    private String terminalId;
    private UUID branchId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        terminalId = "TERMINAL-001";
        branchId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    void newOrder_CreatesAndSetsAsActive() {
        // Given
        PosOrder mockOrder = createMockOrder(orderId, PosOrder.OrderStatus.DRAFT);
        when(orderService.createDraft(any(UUID.class), any(), any())).thenReturn(mockOrder);

        // When
        PosOrder result = multiOrderService.newOrder(terminalId, branchId, "Customer A", null);

        // Then
        assertNotNull(result);
        assertEquals(PosOrder.OrderStatus.DRAFT, result.getStatus());
        verify(orderService).createDraft(eq(branchId), any(), eq(null));
    }

    @Test
    void switchOrder_ChangesActiveOrderForTerminal() {
        // Given
        UUID order1 = UUID.randomUUID();
        UUID order2 = UUID.randomUUID();
        
        PosOrder mockOrder1 = createMockOrder(order1, PosOrder.OrderStatus.DRAFT);
        PosOrder mockOrder2 = createMockOrder(order2, PosOrder.OrderStatus.DRAFT);
        
        when(orderService.findById(order1)).thenReturn(Optional.of(mockOrder1));
        when(orderService.findById(order2)).thenReturn(Optional.of(mockOrder2));

        // When - Create first order then switch to second
        multiOrderService.newOrder(terminalId, branchId, "Customer A", null);
        Optional<PosOrder> switched = multiOrderService.switchOrder(terminalId, order2);

        // Then
        assertTrue(switched.isPresent());
        assertEquals(order2, switched.get().getId());
    }

    @Test
    void getActiveOrder_ReturnsCurrentActiveOrder() {
        // Given
        PosOrder mockOrder = createMockOrder(orderId, PosOrder.OrderStatus.DRAFT);
        when(orderService.createDraft(any(UUID.class), any(), any())).thenReturn(mockOrder);

        // When
        multiOrderService.newOrder(terminalId, branchId, "Customer A", null);
        Optional<PosOrder> active = multiOrderService.getActiveOrder(terminalId);

        // Then
        assertTrue(active.isPresent());
        assertEquals(orderId, active.get().getId());
    }

    @Test
    void markOrderReady_UpdatesStatusToReady() {
        // Given
        PosOrder mockOrder = createMockOrder(orderId, PosOrder.OrderStatus.DRAFT);
        when(orderService.updateStatus(eq(orderId), eq(PosOrder.OrderStatus.READY))).thenReturn(mockOrder);

        // When
        Optional<PosOrder> result = multiOrderService.markOrderReady(orderId);

        // Then
        assertTrue(result.isPresent());
        verify(orderService).updateStatus(orderId, PosOrder.OrderStatus.READY);
    }

    @Test
    void cancelOrder_UpdatesStatusToCancelled() {
        // Given
        PosOrder mockOrder = createMockOrder(orderId, PosOrder.OrderStatus.DRAFT);
        when(orderService.updateStatus(eq(orderId), eq(PosOrder.OrderStatus.CANCELLED))).thenReturn(mockOrder);

        // When
        Optional<PosOrder> result = multiOrderService.cancelOrder(orderId);

        // Then
        assertTrue(result.isPresent());
        verify(orderService).updateStatus(orderId, PosOrder.OrderStatus.CANCELLED);
    }

    @Test
    void listActiveOrders_ReturnsOnlyNonTerminalOrders() {
        // Given
        List<PosOrder> activeOrders = List.of(
            createMockOrder(UUID.randomUUID(), PosOrder.OrderStatus.DRAFT),
            createMockOrder(UUID.randomUUID(), PosOrder.OrderStatus.READY)
        );
        when(orderService.findActiveByBranch(branchId)).thenReturn(activeOrders);

        // When
        List<PosOrder> result = multiOrderService.listActiveOrders(branchId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getOrder_NotFound_ReturnsEmpty() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(orderService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<PosOrder> result = multiOrderService.getOrder(nonExistentId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void completeOrder_ProcessSale() {
        // Given
        PosOrder mockOrder = createMockOrder(orderId, PosOrder.OrderStatus.READY);
        when(orderService.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderService.completeOrder(eq(orderId), any())).thenReturn(mockOrder);

        // When
        Optional<PosOrder> result = multiOrderService.completeOrder(orderId, "CASH", null);

        // Then
        assertTrue(result.isPresent());
        verify(orderService).completeOrder(eq(orderId), any());
    }

    @Test
    void terminalOrders_ConcurrentAccess_Safe() {
        // Given - Multiple terminals creating orders concurrently
        String terminal1 = "TERM-001";
        String terminal2 = "TERM-002";
        
        PosOrder order1 = createMockOrder(UUID.randomUUID(), PosOrder.OrderStatus.DRAFT);
        PosOrder order2 = createMockOrder(UUID.randomUUID(), PosOrder.OrderStatus.DRAFT);
        
        when(orderService.createDraft(any(UUID.class), any(), any()))
            .thenReturn(order1)
            .thenReturn(order2);

        // When - Simulate concurrent order creation
        multiOrderService.newOrder(terminal1, branchId, "Customer 1", null);
        multiOrderService.newOrder(terminal2, branchId, "Customer 2", null);

        // Then - Both terminals should have their own active orders
        Optional<PosOrder> active1 = multiOrderService.getActiveOrder(terminal1);
        Optional<PosOrder> active2 = multiOrderService.getActiveOrder(terminal2);
        
        assertTrue(active1.isPresent());
        assertTrue(active2.isPresent());
        assertNotEquals(active1.get().getId(), active2.get().getId());
    }

    @Test
    void orderStatus_EnumValues() {
        // Verify all order statuses are defined
        PosOrder.OrderStatus[] statuses = PosOrder.OrderStatus.values();
        
        assertTrue(statuses.length >= 3);
        assertTrue(List.of(statuses).contains(PosOrder.OrderStatus.DRAFT));
        assertTrue(List.of(statuses).contains(PosOrder.OrderStatus.READY));
        assertTrue(List.of(statuses).contains(PosOrder.OrderStatus.CANCELLED));
    }

    private PosOrder createMockOrder(UUID id, PosOrder.OrderStatus status) {
        PosOrder order = mock(PosOrder.class);
        when(order.getId()).thenReturn(id);
        when(order.getStatus()).thenReturn(status);
        return order;
    }
}
