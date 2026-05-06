package com.sgf.inventory.domain;

import com.sgf.core.domain.BaseEntity;
import com.sgf.catalog.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "branch_transfers")
public class BranchTransfer extends BaseEntity {

    public enum TransferStatus {
        PENDING,        // Created by source branch
        IN_TRANSIT,     // Confirmed, stock reserved
        RECEIVED,       // Confirmed by destination branch
        CANCELLED,      // Cancelled before receipt
        DISPUTED        // Destination reports discrepancy
    }

    @Column(nullable = false)
    private UUID sourceBranchId;

    @Column(nullable = false)
    private UUID destinationBranchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "received_quantity")
    private Integer receivedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    public BranchTransfer() {
        this.status = TransferStatus.PENDING;
    }

    public UUID getSourceBranchId() { return sourceBranchId; }
    public void setSourceBranchId(UUID sourceBranchId) { this.sourceBranchId = sourceBranchId; }

    public UUID getDestinationBranchId() { return destinationBranchId; }
    public void setDestinationBranchId(UUID destinationBranchId) { this.destinationBranchId = destinationBranchId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }

    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(OffsetDateTime shippedAt) { this.shippedAt = shippedAt; }

    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }
}