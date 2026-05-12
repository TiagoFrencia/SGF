package com.sgf.pos.web;

import com.sgf.pos.domain.PosOrder;
import com.sgf.pos.domain.PosOrder.OrderStatus;
import com.sgf.pos.service.MultiOrderService;
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
    private final MultiOrderService multiOrderService;

    public PosOrderController(PosOrderService orderService, MultiOrderService multiOrderService) {
        this.orderService = orderService;
        this.multiOrderService = multiOrderService;
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
            @PathVariable("orderId") UUID orderId, @RequestBody AddItemRequest request) {
        PosOrder order = orderService.addItem(
                orderId, request.productId(), request.quantity(),
                request.unitPrice(), request.batchId());
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/scan")
    public ResponseEntity<PosOrderResponse> scanAdd(
            @PathVariable("orderId") UUID orderId, @RequestBody ScanRequest request) {
        PosOrder order = orderService.scanAdd(
                orderId, request.gtin(), request.quantity(), request.unitPrice());
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<PosOrderResponse> removeItem(
            @PathVariable("orderId") UUID orderId, @PathVariable("itemId") UUID itemId) {
        PosOrder order = orderService.removeItem(orderId, itemId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @PatchMapping("/{orderId}/ready")
    public ResponseEntity<PosOrderResponse> markReady(@PathVariable("orderId") UUID orderId) {
        PosOrder order = orderService.markReady(orderId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<SaleCompletedResponse> complete(
            @PathVariable("orderId") UUID orderId, @RequestBody CompleteOrderRequest request) {
        UUID saleId = orderService.completeOrder(
                orderId, request.paymentMethod(), request.idempotencyKey(),
                request.pamiPrescriptionId(), request.pamiBeneficiaryId(),
                request.doctorLicense(), request.doctorRegion());
        return ResponseEntity.ok(new SaleCompletedResponse(
                saleId, request.idempotencyKey(), "COMPLETED",
                null, 0, null, request.paymentMethod(), List.of()));
    }

    @PatchMapping("/{orderId}/void")
    public ResponseEntity<PosOrderResponse> voidOrder(@PathVariable("orderId") UUID orderId) {
        PosOrder order = orderService.voidOrder(orderId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PosOrderResponse> getOrder(@PathVariable("orderId") UUID orderId) {
        PosOrder order = orderService.findById(orderId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<List<PosOrderResponse>> listOpen(
            @RequestParam(name = "branchId") UUID branchId,
            @RequestParam(name = "status", required = false) OrderStatus status) {
        if (status != null) {
            var filtered = orderService.listOpenOrders(branchId).stream()
                    .filter(order -> order.getStatus() == status)
                    .map(PosOrderResponse::from)
                    .toList();
            return ResponseEntity.ok(filtered);
        }
        return ResponseEntity.ok(orderService.listOpenOrders(branchId).stream()
                .map(PosOrderResponse::from).toList());
    }

    @PostMapping("/terminals/{terminalId}/new")
    public ResponseEntity<PosOrderResponse> newTerminalOrder(
            @PathVariable("terminalId") String terminalId,
            @RequestBody CreateDraftRequest request) {
        PosOrder order = multiOrderService.newOrder(
                terminalId,
                request.branchId(),
                request.customerName(),
                request.customerDocument(),
                request.notes()
        );
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @PatchMapping("/terminals/{terminalId}/switch/{orderId}")
    public ResponseEntity<PosOrderResponse> switchTerminalOrder(
            @PathVariable("terminalId") String terminalId,
            @PathVariable("orderId") UUID orderId) {
        PosOrder order = multiOrderService.switchTo(terminalId, orderId);
        return ResponseEntity.ok(PosOrderResponse.from(order));
    }

    @GetMapping("/terminals/{terminalId}/active")
    public ResponseEntity<PosOrderResponse> getTerminalActiveOrder(@PathVariable("terminalId") String terminalId) {
        return multiOrderService.getActive(terminalId)
                .map(PosOrderResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/terminals/{terminalId}")
    public ResponseEntity<List<PosOrderResponse>> listTerminalOrders(@PathVariable("terminalId") String terminalId) {
        return ResponseEntity.ok(multiOrderService.listOpen(terminalId).stream()
                .map(PosOrderResponse::from)
                .toList());
    }

    @DeleteMapping("/terminals/{terminalId}")
    public ResponseEntity<Void> closeTerminal(@PathVariable("terminalId") String terminalId) {
        multiOrderService.closeTerminal(terminalId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/terminals/{terminalId}/recover")
    public ResponseEntity<TerminalRecoveryResponse> recoverTerminal(
            @PathVariable("terminalId") String terminalId,
            @RequestParam(name = "branchId") UUID branchId) {
        int recovered = multiOrderService.recoverTerminal(terminalId, branchId);
        Optional<UUID> active = multiOrderService.getActiveOrderId(terminalId);
        return ResponseEntity.ok(new TerminalRecoveryResponse(
                terminalId,
                branchId,
                recovered,
                active.orElse(null)
        ));
    }

    @DeleteMapping("/terminals/{terminalId}/orders/{orderId}")
    public ResponseEntity<Void> removeTerminalOrder(
            @PathVariable("terminalId") String terminalId,
            @PathVariable("orderId") UUID orderId) {
        multiOrderService.removeFromTerminal(terminalId, orderId);
        return ResponseEntity.noContent().build();
    }

    // --- Request DTOs ---

    public record CreateDraftRequest(UUID branchId, String customerName,
                                      String customerDocument, String notes) {}

    public record AddItemRequest(UUID productId, int quantity, BigDecimal unitPrice,
                                  UUID batchId) {}

    public record ScanRequest(String gtin, int quantity, BigDecimal unitPrice) {}

    public record CompleteOrderRequest(String paymentMethod, String idempotencyKey,
                                       String pamiPrescriptionId, String pamiBeneficiaryId,
                                       String doctorLicense, String doctorRegion) {}

    public record TerminalRecoveryResponse(
            String terminalId,
            UUID branchId,
            int recoveredOrders,
            UUID activeOrderId
    ) {}

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
                                    item.getProduct().getGtin(),
                                    item.getProduct().getTroquel(),
                                    item.getProduct().getSource(),
                                    item.getProduct().getSourceUpdatedAt(),
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
            String gtin, String troquel, String productSource, java.time.LocalDate productSourceUpdatedAt,
            int quantity, BigDecimal unitPrice, BigDecimal subtotal, UUID batchId
    ) {}
}
