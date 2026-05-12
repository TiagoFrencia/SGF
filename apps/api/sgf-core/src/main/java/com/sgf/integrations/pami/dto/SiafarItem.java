package com.sgf.integrations.pami.dto;

import java.math.BigDecimal;

/**
 * Medicamento individual en la receta SIAFAR.
 */
public record SiafarItem(
        String troquelCode,    // CodTroquel
        int requestedQuantity, // CantidadSolicitada
        BigDecimal unitPrice   // ImporteUnitario
) {
}
