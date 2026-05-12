package com.sgf.integrations.pami.dto;

import java.util.List;

/**
 * Respuesta de validación SIAFAR.
 */
public record SiafarValidationResponse(
        String responseCode,      // CodRespuesta: 0=Éxito
        String responseMessage,   // Mensaje de error/éxito
        String authorizationNumber, // NroAutorizacion
        List<SiafarValidationItemResponse> items
) {
    public boolean isApproved() {
        return "0".equals(responseCode);
    }

    public record SiafarValidationItemResponse(
            String troquelCode,
            String status,        // A=Aprobado, R=Rechazado
            String statusMessage
    ) {}
}
