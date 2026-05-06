package com.sgf.pos.service;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.service.ProductService;
import com.sgf.core.domain.ConflictException;
import com.sgf.core.domain.NotFoundException;
import com.sgf.inventory.service.InventoryService;
import com.sgf.pos.domain.PosOrder;
import com.sgf.pos.domain.PosOrder.OrderStatus;
import com.sgf.pos.domain.PosOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages POS order lifecycle: create, add items, ready, complete (→ sale), void.
 * Supports multiple simultaneous open orders per branch.
 */
@Service
@Transactional
public class PosOrderService {

    private static final Logger log = LoggerFactory.getLogger(PosOrderService.class);

    private final PosOrderRepository orderRepository;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final SalesService salesService;

    public PosOrderService(PosOrderRepository orderRepository,
                           ProductService productService,
                           InventoryService inventoryService,
                           SalesService salesService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.salesService = salesService;
    }

    /**
     * Create a new draft order for a branch. Auto-increments order number per branch per day.
     */
    public PosOrder createDraft(UUID branchId, String customerName, String customerDocument, String notes) {
        PosOrder order = new PosOrder();
        order.setBranchId(branchId);
        order.setCustomerName(customerName);
        order.setCustomerDocument(customerDocument);
        order.setNotes(notes);
        order.setOrderNumber(nextOrderNumber(branchId));
        return orderRepository.save(order);
    }

    /**
     * Add a product to an open draft order. Recalculates total automatically.
     */
    public PosOrder addItem(UUID orderId, UUID productId, int quantity, BigDecimal unitPrice, UUID batchId) {
        PosOrder order = findDraft(orderId);
        Product product = productService.findEntity(productId);
        order.addItem(product, quantity, unitPrice, batchId);
        return orderRepository.save(order);
    }

    /**
     * Remove an item from a draft order.
     */
    public PosOrder removeItem(UUID orderId, UUID itemId) {
        PosOrder order = findDraft(orderId);
        order.removeItem(itemId);
        return orderRepository.save(order);
    }

    /**
     * Quick product lookup by GTIN: scans and adds directly.
     * The "3-click" POS flow: scan → auto-add → ready → pay.
     */
    public PosOrder scanAdd(UUID orderId, String gtin, int quantity, BigDecimal unitPrice) {
        Product product = productService.findByGtin(gtin);
        return addItem(orderId, product.getId(), quantity, unitPrice, null);
    }

    /**
     * Mark order as ready to pay. Locks items from further modification.
     */
    public PosOrder markReady(UUID orderId) {
        PosOrder order = findDraft(orderId);
        order.markReady();
        log.info("Order {} marked READY, total={}", order.getId(), order.getTotalAmount());
        return orderRepository.save(order);
    }

    /**
     * Complete order: convert to sale, reserve inventory, trigger AFIP/ANMAT.
     * Returns the sale ID for the completed transaction.
     */
    public UUID completeOrder(UUID orderId, String paymentMethod, String idempotencyKey) {
        PosOrder order = findReady(orderId);

        // Convert order items to sale items DTO
        var saleItems = order.getItems().stream()
                .map(item -> new SalesService.SaleItemRequest(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        // Create sale through standard sales pipeline
        var saleReq = new SalesService.SaleRequest(
                idempotencyKey != null ? idempotencyKey : "pos-order-" + orderId,
                saleItems,
                paymentMethod,
                order.getCustomerDocument()
        );
        var saleResult = salesService.create(saleReq, "pos-operator");

        // Mark order as completed
        order.markCompleted();
        orderRepository.save(order);

        log.info("Order {} completed → sale {}", orderId, saleResult.saleId());
        return saleResult.saleId();
    }

    /**
     * Void a draft or ready order.
     */
    public PosOrder voidOrder(UUID orderId) {
        PosOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        order.markVoided();
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<PosOrder> listOpenOrders(UUID branchId) {
        return orderRepository.findByBranchIdAndStatusIn(branchId,
                List.of(OrderStatus.DRAFT, OrderStatus.READY));
    }

    @Transactional(readOnly = true)
    public List<PosOrder> listDrafts(UUID branchId) {
        return orderRepository.findByBranchIdAndStatus(branchId, OrderStatus.DRAFT);
    }

    @Transactional(readOnly = true)
    public PosOrder findById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    private PosOrder findDraft(UUID orderId) {
        PosOrder order = orderRepository.findByIdAndStatus(orderId, OrderStatus.DRAFT)
                .orElseThrow(() -> new ConflictException("Order " + orderId + " is not in DRAFT status or does not exist"));
        return order;
    }

    private PosOrder findReady(UUID orderId) {
        return orderRepository.findByIdAndStatus(orderId, OrderStatus.READY)
                .orElseThrow(() -> new ConflictException("Order " + orderId + " is not in READY status or does not exist"));
    }

    private int nextOrderNumber(UUID branchId) {
        var last = orderRepository.findTopByBranchIdAndStatusOrderByOrderNumberDesc(branchId, OrderStatus.DRAFT);
        return last.map(o -> o.getOrderNumber() + 1).orElse(1);
    }
}