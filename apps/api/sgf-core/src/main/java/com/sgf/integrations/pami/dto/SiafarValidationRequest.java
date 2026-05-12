package com.sgf.integrations.pami.dto;

import java.util.List;

/**
 * Petición completa de validación ADESFA para SIAFAR.
 */
public record SiafarValidationRequest(
        SiafarMessageHeader header,
        SiafarPrescriptionHeader prescription,
        List<SiafarItem> items
) {
}
