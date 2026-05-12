package com.sgf.integrations.refeps.dto;

/**
 * Representa la información de un profesional de la salud obtenida de REFEPS (SISA).
 */
public record ProfessionalLicenseResponse(
        String licenseNumber,    // Nro Matrícula
        String licenseType,      // Nacional / Provincial
        String fullName,         // Nombre y Apellido
        String profession,       // Médico, Farmacéutico, etc.
        String status,           // Habilitado, Suspendido, Fallecido
        boolean isValid          // Resultado de la validación lógica
) {
    public static ProfessionalLicenseResponse invalid(String licenseNumber) {
        return new ProfessionalLicenseResponse(licenseNumber, null, null, null, "No encontrado", false);
    }
}
