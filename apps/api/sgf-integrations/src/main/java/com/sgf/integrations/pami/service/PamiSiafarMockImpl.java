package com.sgf.integrations.pami.service;

import com.sgf.integrations.pami.dto.SiafarValidationRequest;
import com.sgf.integrations.pami.dto.SiafarValidationResponse;
import com.sgf.integrations.pami.dto.SiafarValidationResponse.SiafarValidationItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementación Mock para desarrollo y pruebas sin conexión real a SIAFAR.
 */
@Service
@ConditionalOnProperty(name = "app.integrations.pami.mode", havingValue = "mock", matchIfMissing = true)
public class PamiSiafarMockImpl implements PamiSiafarService {

    private static final Logger log = LoggerFactory.getLogger(PamiSiafarMockImpl.class);

    @Override
    public SiafarValidationResponse validatePrescription(SiafarValidationRequest request) {
        log.info("MOCK PAMI: Validating prescription {} for beneficiary {}", 
                request.prescription().prescriptionId(), request.prescription().beneficiaryId());

        // Simulación: si el nro de receta empieza con '9', simulamos un rechazo
        if (request.prescription().prescriptionId().startsWith("9")) {
            return new SiafarValidationResponse(
                    "100",
                    "ERROR: Receta inexistente o ya validada",
                    null,
                    List.of()
            );
        }

        List<SiafarValidationItemResponse> items = request.items().stream()
                .map(item -> new SiafarValidationItemResponse(item.troquelCode(), "A", "Aprobado"))
                .toList();

        return new SiafarValidationResponse(
                "0",
                "Operación Exitosa",
                "AUT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                items
        );
    }

    @Override
    public boolean voidValidation(String authorizationNumber) {
        log.info("MOCK PAMI: Voiding authorization {}", authorizationNumber);
        return true;
    }
}
