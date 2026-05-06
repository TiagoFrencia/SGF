package com.sgf.integrations.fhir.profiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FHIR RecetaDigital — CORE-AR prescription profile.
 *
 * Based on HL7 Argentina Receta Digital Electrónica (Ley 27.553).
 * This resource captures the complete electronic prescription:
 * - CUIR (Código Único de Identificación de Receta)
 * - Prescriber (REFEPS-validated professional)
 * - Patient reference (FHIR Patient resource)
 * - Medications prescribed (MedicationRequest array)
 * - Dispense tracking per pharmacy
 * - Validity period (default 30 days for regular, 7 for psychotropics, 3 for opioids)
 *
 * See: https://www.argentina.gob.ar/salud/receta-digital
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecetaDigital {

    private String resourceType = "Bundle";
    private String id;
    private String type = "document"; // FHIR Bundle type
    private String timestamp; // ISO instant when created
    private Identifier identifier; // CUIR
    private List<Entry> entry;
    private Meta meta;

    // --- Argentine extensions ---

    @JsonProperty("_cuir")
    private String cuir; // Código Único de Identificación de Receta (UUID v5 or numeric)

    @JsonProperty("_fechaEmision")
    private LocalDateTime fechaEmision;

    @JsonProperty("_fechaVencimiento")
    private LocalDate fechaVencimiento; // Default: emision + 30 days

    @JsonProperty("_prescriptorId")
    private String prescriptorId; // REFEPS matrícula

    @JsonProperty("_diagnostico")
    private String diagnostico; // Free text or CIE-10 code

    @JsonProperty("_indicaciones")
    private String indicaciones; // Pharmacist instructions

    @JsonProperty("_tipoReceta")
    private String tipoReceta; // AMBULATORIA | ONCOLOGICA | ALTO_COSTO | PSICOTROPICO | ESTUPEFACIENTE

    @JsonProperty("_estado")
    private String estado; // ACTIVA | DISPENSADA_PARCIAL | DISPENSADA_TOTAL | VENCIDA | ANULADA

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Identifier getIdentifier() { return identifier; }
    public void setIdentifier(Identifier identifier) { this.identifier = identifier; }

    public List<Entry> getEntry() { return entry; }
    public void setEntry(List<Entry> entry) { this.entry = entry; }

    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }

    public String getCuir() { return cuir; }
    public void setCuir(String cuir) { this.cuir = cuir; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getPrescriptorId() { return prescriptorId; }
    public void setPrescriptorId(String prescriptorId) { this.prescriptorId = prescriptorId; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String indicaciones) { this.indicaciones = indicaciones; }

    public String getTipoReceta() { return tipoReceta; }
    public void setTipoReceta(String tipoReceta) { this.tipoReceta = tipoReceta; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    // --- Nested types ---

    public static class Identifier {
        private String system; // Usually: https://www.argentina.gob.ar/salud/renapdis/cuir
        private String value;  // The CUIR itself

        public Identifier() {}
        public Identifier(String system, String value) { this.system = system; this.value = value; }

        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class Entry {
        private String fullUrl;
        private ResourceWrapper resource;

        public String getFullUrl() { return fullUrl; }
        public void setFullUrl(String fullUrl) { this.fullUrl = fullUrl; }
        public ResourceWrapper getResource() { return resource; }
        public void setResource(ResourceWrapper resource) { this.resource = resource; }
    }

    public static class ResourceWrapper {
        private String resourceType; // Patient | Practitioner | MedicationRequest | MedicationDispense
        private Object content; // The actual resource (polymorphic in FHIR)

        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public Object getContent() { return content; }
        public void setContent(Object content) { this.content = content; }
    }

    /**
     * MedicationRequest within a prescription.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MedicationRequest {
        private String resourceType = "MedicationRequest";
        private String id;
        private String status; // active | completed | stopped
        private String intent = "order";
        private CodeableConcept medicationCodeableConcept;
        private DosageInstruction dosageInstruction;
        private Quantity dispenseRequest;

        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }

        public CodeableConcept getMedicationCodeableConcept() { return medicationCodeableConcept; }
        public void setMedicationCodeableConcept(CodeableConcept medicationCodeableConcept) {
            this.medicationCodeableConcept = medicationCodeableConcept;
        }

        public DosageInstruction getDosageInstruction() { return dosageInstruction; }
        public void setDosageInstruction(DosageInstruction dosageInstruction) {
            this.dosageInstruction = dosageInstruction;
        }

        public Quantity getDispenseRequest() { return dispenseRequest; }
        public void setDispenseRequest(Quantity dispenseRequest) { this.dispenseRequest = dispenseRequest; }
    }

    public static class CodeableConcept {
        private List<Coding> coding;
        private String text;

        public List<Coding> getCoding() { return coding; }
        public void setCoding(List<Coding> coding) { this.coding = coding; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class Coding {
        private String system;
        private String code;
        private String display;

        public Coding() {}
        public Coding(String system, String code, String display) {
            this.system = system; this.code = code; this.display = display;
        }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDisplay() { return display; }
        public void setDisplay(String display) { this.display = display; }
    }

    public static class DosageInstruction {
        private String text; // e.g., "Tomar 1 comprimido cada 8 horas por 7 días"
        private Timing timing;
        private Quantity doseAndRate;
        private CodeableConcept route;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Timing getTiming() { return timing; }
        public void setTiming(Timing timing) { this.timing = timing; }
        public Quantity getDoseAndRate() { return doseAndRate; }
        public void setDoseAndRate(Quantity doseAndRate) { this.doseAndRate = doseAndRate; }
        public CodeableConcept getRoute() { return route; }
        public void setRoute(CodeableConcept route) { this.route = route; }
    }

    public static class Timing {
        private Repeat repeat;

        public Repeat getRepeat() { return repeat; }
        public void setRepeat(Repeat repeat) { this.repeat = repeat; }
    }

    public static class Repeat {
        private Integer frequency; // times per period
        private Double period;
        private String periodUnit; // h | d | wk
        private Integer duration;
        private String durationUnit; // d | wk

        public Integer getFrequency() { return frequency; }
        public void setFrequency(Integer frequency) { this.frequency = frequency; }
        public Double getPeriod() { return period; }
        public void setPeriod(Double period) { this.period = period; }
        public String getPeriodUnit() { return periodUnit; }
        public void setPeriodUnit(String periodUnit) { this.periodUnit = periodUnit; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        public String getDurationUnit() { return durationUnit; }
        public void setDurationUnit(String durationUnit) { this.durationUnit = durationUnit; }
    }

    public static class Quantity {
        private Double value;
        private String unit;
        private String system;
        private String code;

        public Quantity() {}
        public Quantity(Double value, String unit) { this.value = value; this.unit = unit; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class Meta {
        private String versionId;
        private String lastUpdated;
        private List<String> profile;

        public String getVersionId() { return versionId; }
        public void setVersionId(String versionId) { this.versionId = versionId; }
        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
        public List<String> getProfile() { return profile; }
        public void setProfile(List<String> profile) { this.profile = profile; }
    }
}