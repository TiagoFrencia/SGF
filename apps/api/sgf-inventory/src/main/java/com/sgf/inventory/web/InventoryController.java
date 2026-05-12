package com.sgf.inventory.web;
import org.springframework.http.ResponseEntity;


import com.sgf.inventory.domain.BatchRepository;
import com.sgf.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final BatchRepository batchRepository;

    public InventoryController(InventoryService inventoryService, BatchRepository batchRepository) {
        this.inventoryService = inventoryService;
        this.batchRepository = batchRepository;
    }

    @PostMapping("/receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public InventoryReceiptResponse receive(@Valid @RequestBody InventoryReceiptRequest request,
                                            java.security.Principal principal) {
        return inventoryService.receive(request, principal.getName());
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public List<StockViewResponse> stock() {
        return inventoryService.stock();
    }

    @GetMapping("/products/{productId}/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public List<InventoryReceiptResponse> batches(@PathVariable("productId") UUID productId) {
        return batchRepository.findByProductIdAndAvailableQuantityGreaterThan(productId, 0).stream()
                .sorted(java.util.Comparator.comparing(com.sgf.inventory.domain.Batch::getExpiresAt))
                .map(InventoryReceiptResponse::from)
                .toList();
    }
}
