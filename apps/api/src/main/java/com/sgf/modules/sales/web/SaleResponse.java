package com.sgf.modules.sales.web;

import com.sgf.modules.sales.domain.Sale;
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

