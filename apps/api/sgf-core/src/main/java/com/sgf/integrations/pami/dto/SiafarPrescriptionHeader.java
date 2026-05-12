package com.sgf.integrations.pami.dto;

import java.time.LocalDate;

/**
 * Datos de la receta y el afiliado para SIAFAR.
 */
public record SiafarPrescriptionHeader(
        String financierCode,  // Financiador: PAMI
        String beneficiaryId,  // Credencial/Numero
        String planCode,       // Plan
        String doctorLicense,  // Prescriptor/NroMatricula
        String prescriptionId, // Formulario/Numero
        LocalDate prescriptionDate, // FechaReceta
        String treatmentType   // TipoTratamiento: N o P
) {
}
