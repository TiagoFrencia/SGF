package com.sgf.pos.web;

import com.sgf.pos.domain.Sale;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleCompletedResponse(
        UUID saleId,
        String idempotencyKey,
        String status,
        BigDecimal totalAmount,
        int itemCount,
        OffsetDateTime completedAt,
        String paymentMethod,
        List<SaleCompletedItem> items
) {
    public static SaleCompletedResponse from(Sale sale) {
        return new SaleCompletedResponse(
                sale.getId(),
                sale.getExternalIdempotencyKey(),
                sale.getStatus(),
                sale.getTotalAmount(),
                sale.getItems().size(),
                sale.getSoldAt(),
                sale.getPaymentMethod(),
                sale.getItems().stream()
                        .map(item -> new SaleCompletedItem(
                                item.getProduct().getId(),
                                item.getProduct().getCommercialName(),
                                item.getProduct().getGtin(),
                                item.getProduct().getTroquel(),
                                item.getBatch() != null ? item.getBatch().getLotNumber() : null,
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getSubtotal()))
                        .toList()
        );
    }

    public record SaleCompletedItem(
            UUID productId,
            String productName,
            String gtin,
            String troquel,
            String lotNumber,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
