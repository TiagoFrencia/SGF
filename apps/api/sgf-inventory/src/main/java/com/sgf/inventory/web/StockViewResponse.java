package com.sgf.inventory.web;

import com.sgf.inventory.domain.Batch;
import java.time.LocalDate;
import java.util.UUID;

public record StockViewResponse(
        UUID batchId,
        UUID productId,
        String productName,
        String sku,
        String lotNumber,
        LocalDate expiresAt,
        Integer availableQuantity
) {
    public static StockViewResponse from(Batch batch) {
        return new StockViewResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getProduct().getCommercialName(),
                batch.getProduct().getSku(),
                batch.getLotNumber(),
                batch.getExpiresAt(),
                batch.getAvailableQuantity()
        );
    }
}

