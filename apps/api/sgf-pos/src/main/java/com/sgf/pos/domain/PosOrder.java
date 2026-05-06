package com.sgf.pos.domain;

import com.sgf.core.domain.BaseEntity;
import com.sgf.catalog.domain.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an open order at the POS terminal.
 * Multiple orders can be open simultaneously (e.g., customer browsing vs. ready to pay).
 * Only one is the "active" order at any time, but all are persisted for session recovery.
 */
@Entity
@Table(name = "pos_orders")
public class PosOrder extends BaseEntity {

    public enum OrderStatus {
        DRAFT,          // Being built — items can be modified
        READY,          // Ready to pay — items locked
        COMPLETED,      // Paid and converted to sale
        VOIDED          // Cancelled by operator
    }

    @Column(nullable = false)
    private UUID branchId;

    @Column(name = "order_number", nullable = false)
    private int orderNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_document")
    private String customerDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PosOrderItem> items = new ArrayList<>();

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public PosOrder() {
        this.status = OrderStatus.DRAFT;
    }

    /**
     * Add or update an item line in this order.
     * Handles qty stacking: if same product+batch, increments qty instead of duplicating.
     */
    public PosOrderItem addItem(Product product, int quantity, BigDecimal unitPrice, UUID batchId) {
        // Stack if same product + batch already in order
        for (PosOrderItem item : items) {
            if (item.getProduct().getId().equals(product.getId())
                    && (batchId == null || batchId.equals(item.getBatchId()))) {
                item.setQuantity(item.getQuantity() + quantity);
                item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                recalculate();
                return item;
            }
        }
        PosOrderItem item = new PosOrderItem();
        item.setOrder(this);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setBatchId(batchId);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        items.add(item);
        recalculate();
        return item;
    }

    public void removeItem(UUID itemId) {
        items.removeIf(item -> item.getId() != null && item.getId().equals(itemId));
        recalculate();
    }

    public void recalculate() {
        this.totalAmount = items.stream()
                .map(PosOrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void markReady() {
        if (this.status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Order must be DRAFT to mark READY");
        }
        if (this.items.isEmpty()) {
            throw new IllegalStateException("Cannot mark empty order as READY");
        }
        this.status = OrderStatus.READY;
    }

    public void markCompleted() {
        if (this.status != OrderStatus.READY) {
            throw new IllegalStateException("Order must be READY to complete");
        }
        this.status = OrderStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public void markVoided() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot void a completed order");
        }
        this.status = OrderStatus.VOIDED;
    }

    // Getters and setters
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }

    public int getOrderNumber() { return orderNumber; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerDocument() { return customerDocument; }
    public void setCustomerDocument(String customerDocument) { this.customerDocument = customerDocument; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<PosOrderItem> getItems() { return items; }
    public void setItems(List<PosOrderItem> items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}