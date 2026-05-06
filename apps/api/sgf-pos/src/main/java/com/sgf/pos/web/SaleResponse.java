package com.sgf.pos.web;

import com.sgf.pos.domain.Sale;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID saleId,
        String idempotencyKey,
        String status,
        BigDecimal totalAmount,
        OffsetDateTime soldAt,
        List<SaleItemResponse> items
) {
    public static SaleResponse fromLegacy(SaleCompletedResponse completed) {
        return new SaleResponse(
                completed.saleId(),
                completed.idempotencyKey(),
                completed.status(),
                completed.totalAmount(),
                completed.completedAt(),
                completed.items().stream()
                        .map(i -> new SaleItemResponse(
                                i.productId(), i.productName(), i.lotNumber(),
                                i.quantity(), i.unitPrice(), i.subtotal()))
                        .toList()
        );
    }

    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getExternalIdempotencyKey(),
                sale.getStatus(),
                sale.getTotalAmount(),
                sale.getSoldAt(),
                sale.getItems().stream()
                        .map(item -> new SaleItemResponse(
                                item.getProduct().getId(),
                                item.getProduct().getCommercialName(),
                                item.getBatch().getLotNumber(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getSubtotal()
                        ))
                        .toList()
        );
    }

    public record SaleItemResponse(
            UUID productId,
            String productName,
            String lotNumber,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }
}

