package com.sgf.inventory.web;

import com.sgf.inventory.domain.BranchTransfer;
import com.sgf.inventory.domain.BranchTransfer.TransferStatus;
import com.sgf.inventory.service.BranchTransferService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory/transfers")
public class BranchTransferController {

    private final BranchTransferService transferService;

    public BranchTransferController(BranchTransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<BranchTransferResponse> create(@RequestBody CreateTransferRequest request) {
        return ResponseEntity.ok(BranchTransferResponse.from(transferService.create(
                request.sourceBranchId(), request.destinationBranchId(),
                request.productId(), request.batchId(), request.quantity(),
                request.notes(), "current-user" // TODO: extract from JWT
        )));
    }

    @PatchMapping("/{id}/ship")
    public ResponseEntity<BranchTransferResponse> ship(@PathVariable("id") UUID id, @RequestBody BranchActionRequest request) {
        return ResponseEntity.ok(BranchTransferResponse.from(transferService.ship(id, request.branchId(), "current-user")));
    }

    @PatchMapping("/{id}/receive")
    public ResponseEntity<BranchTransferResponse> receive(@PathVariable("id") UUID id, @RequestBody ReceiveRequest request) {
        return ResponseEntity.ok(BranchTransferResponse.from(transferService.receive(id, request.branchId(), request.receivedQuantity(), "current-user")));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BranchTransferResponse> cancel(@PathVariable("id") UUID id, @RequestBody BranchActionRequest request) {
        return ResponseEntity.ok(BranchTransferResponse.from(transferService.cancel(id, request.branchId(), "current-user")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchTransferResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(BranchTransferResponse.from(transferService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<BranchTransferResponse>> list(
            @RequestParam(name = "sourceBranchId", required = false) UUID sourceBranchId,
            @RequestParam(name = "destinationBranchId", required = false) UUID destinationBranchId,
            @RequestParam(name = "status", required = false) TransferStatus status) {
        if (sourceBranchId != null) {
            return ResponseEntity.ok(transferService.listBySourceBranch(sourceBranchId, status).stream()
                    .map(BranchTransferResponse::from)
                    .toList());
        }
        if (destinationBranchId != null) {
            return ResponseEntity.ok(transferService.listByDestinationBranch(destinationBranchId, status).stream()
                    .map(BranchTransferResponse::from)
                    .toList());
        }
        return ResponseEntity.ok(List.of());
    }

    public record CreateTransferRequest(
            UUID sourceBranchId,
            UUID destinationBranchId,
            UUID productId,
            UUID batchId,
            int quantity,
            String notes
    ) {}

    public record BranchActionRequest(UUID branchId) {}

    public record ReceiveRequest(UUID branchId, Integer receivedQuantity) {}

    public record BranchTransferResponse(
            UUID id,
            UUID sourceBranchId,
            UUID destinationBranchId,
            UUID productId,
            String productName,
            UUID batchId,
            String lotNumber,
            int quantity,
            Integer receivedQuantity,
            TransferStatus status,
            String notes,
            OffsetDateTime shippedAt,
            OffsetDateTime receivedAt
    ) {
        public static BranchTransferResponse from(BranchTransfer transfer) {
            return new BranchTransferResponse(
                    transfer.getId(),
                    transfer.getSourceBranchId(),
                    transfer.getDestinationBranchId(),
                    transfer.getProduct().getId(),
                    transfer.getProduct().getCommercialName(),
                    transfer.getBatch().getId(),
                    transfer.getBatch().getLotNumber(),
                    transfer.getQuantity(),
                    transfer.getReceivedQuantity(),
                    transfer.getStatus(),
                    transfer.getNotes(),
                    transfer.getShippedAt(),
                    transfer.getReceivedAt()
            );
        }
    }
}
