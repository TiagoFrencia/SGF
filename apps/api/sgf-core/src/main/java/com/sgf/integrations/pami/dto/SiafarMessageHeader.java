package com.sgf.integrations.pami.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Representa la cabecera técnica del mensaje ADESFA 3.1.0 para SIAFAR.
 */
public record SiafarMessageHeader(
        String messageType,    // TipoMsj: 200 (Directa)
        String actionCode,     // CodAccion: 290020 (Validación)
        String messageId,      // IdMsj: UUID
        OffsetDateTime startTimestamp, // InicioTrx
        String softwareId,     // Software
        String validatorName,  // Validador: SIAFAR/COFA
        String providerCode    // Prestador: Código PAMI de la farmacia
) {
    public static SiafarMessageHeader defaultHeader(String pharmacyCode) {
        return new SiafarMessageHeader(
                "200",
                "290020",
                UUID.randomUUID().toString(),
                OffsetDateTime.now(),
                "SGF-ENTERPRISE-2026",
                "SIAFAR",
                pharmacyCode
        );
    }
}
