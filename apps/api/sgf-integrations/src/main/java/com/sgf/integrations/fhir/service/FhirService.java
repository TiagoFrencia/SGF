package com.sgf.integrations.fhir.service;

import com.sgf.catalog.domain.Product;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Service to generate HL7 FHIR CORE-AR resources for interoperability.
 */
@Service
public class FhirService {

    /**
     * Map a Product entity to a FHIR Medication resource.
     */
    public Medication mapToMedication(Product product) {
        Medication med = new Medication();
        med.setId(product.getId().toString());
        
        Identifier gtinId = new Identifier();
        gtinId.setSystem("http://www.anmat.gov.ar/gtin");
        gtinId.setValue(product.getGtin());
        med.getIdentifier().add(gtinId);
        
        CodeableConcept code = new CodeableConcept();
        code.setText(product.getCommercialName());
        code.addCoding(new Coding("http://snomed.info/sct", "placeholder-snomed", product.getCommercialName()));
        med.setCode(code);
        
        med.setStatus(Medication.MedicationStatus.ACTIVE);
        
        return med;
    }

    /**
     * Create a FHIR Patient resource (CORE-AR compliant).
     */
    public Patient createPatient(String dni, String firstName, String lastName, String gender, String birthDate) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        
        Identifier id = new Identifier();
        id.setSystem("http://www.renaper.gob.ar/dni");
        id.setValue(dni);
        patient.getIdentifier().add(id);
        
        patient.addName()
            .setFamily(lastName)
            .addGiven(firstName)
            .setText(firstName + " " + lastName);
        
        if ("M".equalsIgnoreCase(gender)) {
            patient.setGender(Enumerations.AdministrativeGender.MALE);
        } else if ("F".equalsIgnoreCase(gender)) {
            patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        } else {
            patient.setGender(Enumerations.AdministrativeGender.OTHER);
        }
        
        try {
            patient.setBirthDateElement(new DateType(birthDate));
        } catch (Exception e) {
            // Log and ignore
        }
        
        return patient;
    }

    /**
     * Generate a CUIR (Código Único de Identificación de Receta).
     * Format: {PharmacyGLN}-{Date}-{Sequence}
     */
    public String generateCuir(String pharmacyGln) {
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("%s-%s-%s", pharmacyGln, datePart, randomPart);
    }
}
