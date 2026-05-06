package com.sgf.inventory.web;

import com.sgf.modules.auth.service.SgfUserPrincipal;
import com.sgf.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public InventoryReceiptResponse receive(@Valid @RequestBody InventoryReceiptRequest request,
                                            @AuthenticationPrincipal SgfUserPrincipal principal) {
        return inventoryService.receive(request, principal.getUsername());
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public List<StockViewResponse> stock() {
        return inventoryService.stock();
    }
}

