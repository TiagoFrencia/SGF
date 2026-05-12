package com.sgf.core.event;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published when a sale is completed.
 */
public record SaleCompletedEvent(
    UUID saleId,
    String idempotencyKey,
    java.math.BigDecimal totalAmount,
    String actorUsername,
    String tenantId,
    OffsetDateTime occurredAt,
    String paymentMethod,
    String customerDocument,
    String pamiPrescriptionId,
    String pamiBeneficiaryId,
    String doctorLicense,
    String doctorRegion,
    java.util.List<SaleItemInfo> items
) implements DomainEvent {


    public record SaleItemInfo(
            UUID productId,
            String gtin,
            String troquel,
            UUID batchId,
            String lotNumber,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            boolean requiresTraceability
    ) {}
    
    @Override
    public UUID aggregateId() { return saleId; }

    @Override
    public String tenantId() { return tenantId; }

    @Override
    public String aggregateType() { return "SALE"; }

    @Override
    public String eventType() { return "SALE_COMPLETED"; }

    @Override
    public String payload() {
        StringBuilder json = new StringBuilder();
        json.append("{")
                .append("\"saleId\":\"").append(saleId).append("\",")
                .append("\"idempotencyKey\":\"").append(escape(idempotencyKey)).append("\",")
                .append("\"total\":").append(totalAmount).append(",")
                .append("\"paymentMethod\":\"").append(escape(paymentMethod)).append("\",")
                .append("\"customerDocument\":\"").append(escape(customerDocument)).append("\",")
                .append("\"pamiPrescriptionId\":\"").append(escape(pamiPrescriptionId)).append("\",")
                .append("\"pamiBeneficiaryId\":\"").append(escape(pamiBeneficiaryId)).append("\",")
                .append("\"doctorLicense\":\"").append(escape(doctorLicense)).append("\",")
                .append("\"doctorRegion\":\"").append(escape(doctorRegion)).append("\",")
                .append("\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            SaleItemInfo item = items.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{")
                    .append("\"productId\":\"").append(item.productId()).append("\",")
                    .append("\"gtin\":\"").append(escape(item.gtin())).append("\",")
                    .append("\"troquel\":\"").append(escape(item.troquel())).append("\",")
                    .append("\"batchId\":\"").append(item.batchId() != null ? item.batchId() : "").append("\",")
                    .append("\"lotNumber\":\"").append(escape(item.lotNumber())).append("\",")
                    .append("\"quantity\":").append(item.quantity()).append(",")
                    .append("\"unitPrice\":").append(item.unitPrice()).append(",")
                    .append("\"subtotal\":").append(item.subtotal()).append(",")
                    .append("\"requiresTraceability\":").append(item.requiresTraceability())
                    .append("}");
        }
        return json.append("]}").toString();
    }

    @Override
    public OffsetDateTime occurredAt() { return occurredAt; }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
