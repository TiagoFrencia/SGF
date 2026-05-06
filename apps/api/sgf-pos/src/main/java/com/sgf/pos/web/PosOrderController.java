package com.sgf.pos.web;

import com.sgf.pos.domain.PosOrder;
import com.sgf.pos.domain.PosOrder.OrderStatus;
import com.sgf.pos.service.PosOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pos/orders")
public class PosOrderController {

    private final PosOrderService orderService;

    public PosOrderController(PosOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<PosOrderResponse> createDraft(@RequestBody CreateDraftRequest request) {
        PosOrder order = orderService.createDraft(
                request.branchId(), request.customerName(),
                request.customerDocument(), request.notes());
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<PosOrderResponse> addItem(
            @PathVariable UUID orderId, @RequestBody AddItemRequest request) {
        PosOrder order = orderService.addItem(
                orderId, request.productId(), request.quantity(),
                request.unitPrice(), request.batchId());
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/scan")
    public ResponseEntity<PosOrderResponse> scanAdd(
            @PathVariable UUID orderId, @RequestBody ScanRequest request) {
        PosOrder order = orderService.scanAdd(
                orderId, request.gtin(), request.quantity(), request.unitPrice());
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<PosOrderResponse> removeItem(
            @PathVariable UUID orderId, @PathVariable UUID itemId) {
        PosOrder order = orderService.removeItem(orderId, itemId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @PatchMapping("/{orderId}/ready")
    public ResponseEntity<PosOrderResponse> markReady(@PathVariable UUID orderId) {
        PosOrder order = orderService.markReady(orderId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<SaleCompletedResponse> complete(
            @PathVariable UUID orderId, @RequestBody CompleteOrderRequest request) {
        UUID saleId = orderService.completeOrder(
                orderId, request.paymentMethod(), request.idempotencyKey());
        return ResponseEntity.ok(new SaleCompletedResponse(
                saleId, request.idempotencyKey(), "COMPLETED",
                null, 0, null, List.of()));
    }

    @PatchMapping("/{orderId}/void")
    public ResponseEntity<PosOrderResponse> voidOrder(@PathVariable UUID orderId) {
        PosOrder order = orderService.voidOrder(orderId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PosOrderResponse> getOrder(@PathVariable UUID orderId) {
        PosOrder order = orderService.findById(orderId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<List<PosOrderResponse>> listOpen(
            @RequestParam UUID branchId,
            @RequestParam(required = false) OrderStatus status) {
        if (status != null) {
            return ResponseEntity.ok(orderService.listDrafts(branchId).stream()
                    .map(PosOrderResponse::from).toList());
        }
        return ResponseEntity.ok(orderService.listOpenOrders(branchId).stream()
                .map(PosOrderResponse::from).toList());
    }

    // --- Request DTOs ---

    public record CreateDraftRequest(UUID branchId, String customerName,
                                      String customerDocument, String notes) {}

    public record AddItemRequest(UUID productId, int quantity, BigDecimal unitPrice,
                                  UUID batchId) {}

    public record ScanRequest(String gtin, int quantity, BigDecimal unitPrice) {}

    public record CompleteOrderRequest(String paymentMethod, String idempotencyKey) {}

    // --- Response DTO ---

    public record PosOrderResponse(
            UUID orderId,
            int orderNumber,
            OrderStatus status,
            String customerName,
            String customerDocument,
            BigDecimal totalAmount,
            int itemCount,
            List<PosOrderItemResponse> items
    ) {
        public static PosOrderResponse from(PosOrder order) {
            return new PosOrderResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus(),
                    order.getCustomerName(),
                    order.getCustomerDocument(),
                    order.getTotalAmount(),
                    order.getItems().size(),
                    order.getItems().stream()
                            .map(item -> new PosOrderItemResponse(
                                    item.getId(),
                                    item.getProduct().getId(),
                                    item.getProduct().getCommercialName(),
                                    item.getQuantity(),
                                    item.getUnitPrice(),
                                    item.getSubtotal(),
                                    item.getBatchId()))
                            .toList()
            );
        }
    }

    public record PosOrderItemResponse(
            UUID itemId, UUID productId, String productName,
            int quantity, BigDecimal unitPrice, BigDecimal subtotal, UUID batchId
    ) {}
}