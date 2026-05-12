package com.sgf.integrations.refeps.service;

import com.sgf.integrations.refeps.dto.ProfessionalLicenseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementación Mock de REFEPS para desarrollo y homologación.
 */
@Service
@ConditionalOnProperty(name = "app.integrations.refeps.mode", havingValue = "mock", matchIfMissing = true)
public class RefepsMockImpl implements RefepsService {

    private static final Logger log = LoggerFactory.getLogger(RefepsMockImpl.class);

    @Override
    public ProfessionalLicenseResponse validateLicense(String licenseNumber, String region) {
        log.info("MOCK REFEPS: Validating license {} for region {}", licenseNumber, region);

        // Lógica de simulación:
        if (licenseNumber.startsWith("9")) {
            return new ProfessionalLicenseResponse(
                    licenseNumber, 
                    "NAC", 
                    "SISA REJECTED USER", 
                    "MEDICINA", 
                    "SUSPENDIDO", 
                    false
            );
        }

        if (licenseNumber.equals("NOT-FOUND")) {
            return ProfessionalLicenseResponse.invalid(licenseNumber);
        }

        return new ProfessionalLicenseResponse(
                licenseNumber,
                "NAC",
                "DR. TIAGO SGF ENTERPRISE",
                "MEDICINA",
                "HABILITADO",
                true
        );
    }
}
