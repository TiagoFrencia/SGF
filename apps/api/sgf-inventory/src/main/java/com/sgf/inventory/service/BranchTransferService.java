package com.sgf.inventory.service;

import com.sgf.audit.service.AuditService;
import com.sgf.core.domain.ConflictException;
import com.sgf.core.domain.NotFoundException;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import com.sgf.inventory.domain.BranchTransfer;
import com.sgf.inventory.domain.BranchTransfer.TransferStatus;
import com.sgf.inventory.domain.BranchTransferRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BranchTransferService {

    private final BranchTransferRepository transferRepository;
    private final BatchRepository batchRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    public BranchTransferService(BranchTransferRepository transferRepository,
                                 BatchRepository batchRepository,
                                 InventoryService inventoryService,
                                 AuditService auditService) {
        this.transferRepository = transferRepository;
        this.batchRepository = batchRepository;
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
        return findById(saved.getId());
    }

    /**
     * Source branch confirms shipment and the transfer moves to IN_TRANSIT.
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
        transferRepository.save(transfer);
        return findById(transferId);
    }

    /**
     * Destination branch confirms receipt. Quantity mismatch opens a dispute.
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
        transferRepository.save(transfer);
        return findById(transferId);
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
        transferRepository.save(transfer);
        return findById(transferId);
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
        return transferRepository.findBySourceBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public List<BranchTransfer> listByDestinationBranch(UUID branchId, TransferStatus status) {
        if (status != null) {
            return transferRepository.findByDestinationBranchIdAndStatus(branchId, status);
        }
        return transferRepository.findByDestinationBranchId(branchId);
    }

    private Batch findBatchById(UUID batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));
    }
}
