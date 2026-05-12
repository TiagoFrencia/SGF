package com.sgf.integrations.pami.service;

import com.sgf.integrations.pami.dto.SiafarValidationRequest;
import com.sgf.integrations.pami.dto.SiafarValidationResponse;

/**
 * Servicio de interoperabilidad con PAMI SIAFAR.
 */
public interface PamiSiafarService {

    /**
     * Valida una receta online ante SIAFAR.
     */
    SiafarValidationResponse validatePrescription(SiafarValidationRequest request);

    /**
     * Anula una validación previa.
     */
    boolean voidValidation(String authorizationNumber);
}
