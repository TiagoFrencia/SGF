package com.sgf.inventory.service;

import com.sgf.audit.service.AuditService;
import com.sgf.core.domain.ConflictException;
import com.sgf.core.domain.NotFoundException;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BranchTransfer;
import com.sgf.inventory.domain.BranchTransferRepository;
import com.sgf.inventory.domain.BranchTransfer.TransferStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BranchTransferService {

    private final BranchTransferRepository transferRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    public BranchTransferService(BranchTransferRepository transferRepository,
                                  InventoryService inventoryService,
                                  AuditService auditService) {
        this.transferRepository = transferRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
    }

    /**
     * Create a new transfer request. Reserves stock at source branch immediately.
     */
    public BranchTransfer create(UUID sourceBranchId, UUID destinationBranchId,
                                  UUID productId, UUID batchId, int quantity, String notes, String actor) {
        if (sourceBranchId.equals(destinationBranchId)) {
            throw new ConflictException("Source and destination branch must be different");
        }
        if (quantity <= 0) {
            throw new ConflictException("Quantity must be positive");
        }

        // Reserve stock with FEFO
        inventoryService.reserve(productId, quantity, UUID.randomUUID());

        Batch batch = findBatchById(batchId);

        BranchTransfer transfer = new BranchTransfer();
        transfer.setSourceBranchId(sourceBranchId);
        transfer.setDestinationBranchId(destinationBranchId);
        transfer.setProduct(batch.getProduct());
        transfer.setBatch(batch);
        transfer.setQuantity(quantity);
        transfer.setNotes(notes);
        transfer.setStatus(TransferStatus.PENDING);
        BranchTransfer saved = transferRepository.save(transfer);

        auditService.record(actor, "BRANCH_TRANSFER_CREATED", "TRANSFER", saved.getId(),
                "{\"source\":\"" + sourceBranchId + "\",\"dest\":\"" + destinationBranchId + "\",\"qty\":" + quantity + "}");
        return saved;
    }

    /**
     * Source branch confirms shipment — status moves to IN_TRANSIT.
     */
    public BranchTransfer ship(UUID transferId, UUID branchId, String actor) {
        BranchTransfer transfer = findById(transferId);
        if (!transfer.getSourceBranchId().equals(branchId)) {
            throw new ConflictException("Only the source branch can ship this transfer");
        }
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new ConflictException("Transfer is not in PENDING status");
        }
        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setShippedAt(OffsetDateTime.now());
        auditService.record(actor, "BRANCH_TRANSFER_SHIPPED", "TRANSFER", transferId, "{}");
        return transferRepository.save(transfer);
    }

    /**
     * Destination branch confirms receipt. Validates quantity received vs expected.
     */
    public BranchTransfer receive(UUID transferId, UUID branchId, Integer receivedQty, String actor) {
        BranchTransfer transfer = findById(transferId);
        if (!transfer.getDestinationBranchId().equals(branchId)) {
            throw new ConflictException("Only the destination branch can receive this transfer");
        }
        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new ConflictException("Transfer must be IN_TRANSIT to be received");
        }

        int qty = receivedQty != null ? receivedQty : transfer.getQuantity();

        if (qty != transfer.getQuantity()) {
            transfer.setReceivedQuantity(qty);
            transfer.setStatus(TransferStatus.DISPUTED);
            auditService.record(actor, "BRANCH_TRANSFER_DISPUTED", "TRANSFER", transferId,
                    "{\"expected\":" + transfer.getQuantity() + ",\"received\":" + qty + "}");
        } else {
            transfer.setReceivedQuantity(qty);
            transfer.setStatus(TransferStatus.RECEIVED);
            transfer.setReceivedAt(OffsetDateTime.now());
            auditService.record(actor, "BRANCH_TRANSFER_RECEIVED", "TRANSFER", transferId,
                    "{\"qty\":" + qty + "}");
        }
        return transferRepository.save(transfer);
    }

    /**
     * Cancel a transfer before it ships.
     */
    public BranchTransfer cancel(UUID transferId, UUID branchId, String actor) {
        BranchTransfer transfer = findById(transferId);
        if (!transfer.getSourceBranchId().equals(branchId)) {
            throw new ConflictException("Only the source branch can cancel this transfer");
        }
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new ConflictException("Only PENDING transfers can be cancelled");
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        auditService.record(actor, "BRANCH_TRANSFER_CANCELLED", "TRANSFER", transferId, "{}");
        return transferRepository.save(transfer);
    }

    @Transactional(readOnly = true)
    public BranchTransfer findById(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("Transfer not found: " + transferId));
    }

    @Transactional(readOnly = true)
    public List<BranchTransfer> listBySourceBranch(UUID branchId, TransferStatus status) {
        if (status != null) {
            return transferRepository.findBySourceBranchIdAndStatus(branchId, status);
        }
        return transferRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<BranchTransfer> listByDestinationBranch(UUID branchId, TransferStatus status) {
        if (status != null) {
            return transferRepository.findByDestinationBranchIdAndStatus(branchId, status);
        }
        return transferRepository.findAll();
    }

    private Batch findBatchById(UUID batchId) {
        // Delegate to InventoryService batch resolution
        // In a real impl this would be a BatchRepository.findById call
        return new Batch(); // placeholder — actual method goes through inventoryService
    }
}