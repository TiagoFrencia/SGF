package com.sgf.pos.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSaleRequest(
        @NotBlank String idempotencyKey,
        @NotEmpty List<@Valid SaleLineRequest> items,
        String pamiPrescriptionId,
        String pamiBeneficiaryId,
        String doctorLicense,
        String doctorRegion
) {
    public CreateSaleRequest(String idempotencyKey, List<@Valid SaleLineRequest> items) {
        this(idempotencyKey, items, null, null, null, null);
    }

    public record SaleLineRequest(
            @NotNull UUID productId,
            @Min(value = 1, message = "quantity must be positive") Integer quantity,
            @NotNull BigDecimal unitPrice
    ) {
    }
}
