package com.sgf.integrations.refeps.service;

import com.sgf.integrations.refeps.dto.ProfessionalLicenseResponse;

/**
 * Servicio federal para la validación de profesionales de la salud (REFEPS/SISA).
 */
public interface RefepsService {

    /**
     * Consulta la validez de una matrícula profesional.
     * @param licenseNumber Número de matrícula.
     * @param region Código de región/provincia o 'NAC' para nacional.
     */
    ProfessionalLicenseResponse validateLicense(String licenseNumber, String region);
}
