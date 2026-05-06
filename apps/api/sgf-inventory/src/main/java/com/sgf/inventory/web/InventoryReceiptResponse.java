package com.sgf.inventory.web;

import com.sgf.inventory.domain.Batch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryReceiptResponse(
        UUID batchId,
        UUID productId,
        String lotNumber,
        LocalDate expiresAt,
        Integer availableQuantity,
        BigDecimal unitCost
) {
    public static InventoryReceiptResponse from(Batch batch) {
        return new InventoryReceiptResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getLotNumber(),
                batch.getExpiresAt(),
                batch.getAvailableQuantity(),
                batch.getUnitCost()
        );
    }
}

