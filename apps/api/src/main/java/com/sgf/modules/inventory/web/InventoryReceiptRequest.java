package com.sgf.modules.inventory.web;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryReceiptRequest(
        @NotNull UUID productId,
        @NotBlank String lotNumber,
        @NotNull @Future LocalDate expiresAt,
        @Min(value = 1, message = "quantity must be positive") Integer quantity,
        @NotNull BigDecimal unitCost
) {
}

