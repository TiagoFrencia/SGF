package com.sgf.integrations.fhir.renapdis;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Client for ReNaPDiS (Registro Nacional de Prescripción Digital de Salud).
 * Handles submission of FHIR bundles to the national interoperability bus.
 */
@Component
public class RenapdisClient {

    private static final Logger log = LoggerFactory.getLogger(RenapdisClient.class);

    private final IGenericClient client;
    private final FhirContext ctx;

    public RenapdisClient(FhirContext ctx, @Value("${renapdis.server.url:https://sandbox.renapdis.gob.ar/fhir}") String serverUrl) {
        this.ctx = ctx;
        this.client = ctx.newRestfulGenericClient(serverUrl);
    }

    /**
     * Submit a prescription bundle to ReNaPDiS.
     */
    public String submitPrescription(Bundle bundle) {
        log.info("Submitting prescription bundle to ReNaPDiS: {} entries", bundle.getEntry().size());
        
        try {
            Bundle response = client.transaction().withBundle(bundle).execute();
            log.info("Prescription submitted successfully. Response status: {}", response.getType());
            return response.getId();
        } catch (Exception e) {
            log.error("Failed to submit prescription to ReNaPDiS: {}", e.getMessage());
            throw new RuntimeException("ReNaPDiS submission failed", e);
        }
    }

    /**
     * Create a transaction bundle for a new prescription.
     */
    public Bundle createPrescriptionBundle(Patient patient, MedicationRequest request) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        
        bundle.addEntry()
            .setResource(patient)
            .getRequest()
            .setUrl("Patient")
            .setMethod(Bundle.HTTPVerb.POST);
            
        bundle.addEntry()
            .setResource(request)
            .getRequest()
            .setUrl("MedicationRequest")
            .setMethod(Bundle.HTTPVerb.POST);
            
        return bundle;
    }
}
