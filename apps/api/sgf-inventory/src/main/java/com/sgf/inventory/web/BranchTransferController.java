package com.sgf.inventory.web;

import com.sgf.inventory.domain.BranchTransfer;
import com.sgf.inventory.domain.BranchTransfer.TransferStatus;
import com.sgf.inventory.service.BranchTransferService;
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
    public ResponseEntity<BranchTransfer> create(@RequestBody CreateTransferRequest request) {
        return ResponseEntity.ok(transferService.create(
                request.sourceBranchId(), request.destinationBranchId(),
                request.productId(), request.batchId(), request.quantity(),
                request.notes(), "current-user" // TODO: extract from JWT
        ));
    }

    @PatchMapping("/{id}/ship")
    public ResponseEntity<BranchTransfer> ship(@PathVariable UUID id, @RequestBody BranchActionRequest request) {
        return ResponseEntity.ok(transferService.ship(id, request.branchId(), "current-user"));
    }

    @PatchMapping("/{id}/receive")
    public ResponseEntity<BranchTransfer> receive(@PathVariable UUID id, @RequestBody ReceiveRequest request) {
        return ResponseEntity.ok(transferService.receive(id, request.branchId(), request.receivedQuantity(), "current-user"));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BranchTransfer> cancel(@PathVariable UUID id, @RequestBody BranchActionRequest request) {
        return ResponseEntity.ok(transferService.cancel(id, request.branchId(), "current-user"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchTransfer> get(@PathVariable UUID id) {
        return ResponseEntity.ok(transferService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<BranchTransfer>> list(
            @RequestParam(required = false) UUID sourceBranchId,
            @RequestParam(required = false) UUID destinationBranchId,
            @RequestParam(required = false) TransferStatus status) {
        if (sourceBranchId != null) {
            return ResponseEntity.ok(transferService.listBySourceBranch(sourceBranchId, status));
        }
        if (destinationBranchId != null) {
            return ResponseEntity.ok(transferService.listByDestinationBranch(destinationBranchId, status));
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
}