package com.sgf.integrations.fhir.profiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * FHIR MedicamentoAR — CORE-AR profile for medications.
 *
 * Based on HL7 Argentina CORE-AR implementation guide.
 * Extends the base Medication resource with Argentine-specific:
 * - ANMAT registration number
 * - GTIN linkage
 * - Troquel (SIFAR price code)
 * - Active ingredient with controlled substance flag
 * - Pharmaceutical form coded in SNOMED-CT + ANMAT form
 * - Presentation units
 *
 * See: https://www.argentina.gob.ar/salud/renapdis/fhir
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicamentoAR {

    private String resourceType = "Medication";
    private String id;
    private Identifier identifier;
    private CodeableConcept code;
    private String status; // active | inactive | entered-in-error
    private List<Ingredient> ingredient;
    private String form;
    private Amount amount;
    private List<Identifier> batch;
    private Meta meta;

    // --- Argentine extensions ---

    @JsonProperty("_anmatRegistro")
    private String anmatRegistro; // ANMAT certificate number

    @JsonProperty("_troquel")
    private String troquel; // SIFAR price code (6-8 digits)

    @JsonProperty("_presentacion")
    private Integer presentacionUnidades; // Units per package

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Identifier getIdentifier() { return identifier; }
    public void setIdentifier(Identifier identifier) { this.identifier = identifier; }

    public CodeableConcept getCode() { return code; }
    public void setCode(CodeableConcept code) { this.code = code; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Ingredient> getIngredient() { return ingredient; }
    public void setIngredient(List<Ingredient> ingredient) { this.ingredient = ingredient; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public Amount getAmount() { return amount; }
    public void setAmount(Amount amount) { this.amount = amount; }

    public List<Identifier> getBatch() { return batch; }
    public void setBatch(List<Identifier> batch) { this.batch = batch; }

    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }

    public String getAnmatRegistro() { return anmatRegistro; }
    public void setAnmatRegistro(String anmatRegistro) { this.anmatRegistro = anmatRegistro; }

    public String getTroquel() { return troquel; }
    public void setTroquel(String troquel) { this.troquel = troquel; }

    public Integer getPresentacionUnidades() { return presentacionUnidades; }
    public void setPresentacionUnidades(Integer presentacionUnidades) { this.presentacionUnidades = presentacionUnidades; }

    // --- Nested FHIR types ---

    public static class Identifier {
        private String system;
        private String value;

        public Identifier() {}
        public Identifier(String system, String value) { this.system = system; this.value = value; }

        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class CodeableConcept {
        private List<Coding> coding;
        private String text;

        public CodeableConcept() {}
        public CodeableConcept(List<Coding> coding, String text) { this.coding = coding; this.text = text; }

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

    public static class Ingredient {
        private CodeableConcept itemCodeableConcept;
        private Ratio strength;
        private Boolean isActive;
        private CodeableConcept ingredientType; // ACTI (active), INAC (inactive), CONTROLLED

        public CodeableConcept getItemCodeableConcept() { return itemCodeableConcept; }
        public void setItemCodeableConcept(CodeableConcept itemCodeableConcept) {
            this.itemCodeableConcept = itemCodeableConcept;
        }
        public Ratio getStrength() { return strength; }
        public void setStrength(Ratio strength) { this.strength = strength; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public CodeableConcept getIngredientType() { return ingredientType; }
        public void setIngredientType(CodeableConcept ingredientType) { this.ingredientType = ingredientType; }
    }

    public static class Ratio {
        private Quantity numerator;
        private Quantity denominator;

        public Quantity getNumerator() { return numerator; }
        public void setNumerator(Quantity numerator) { this.numerator = numerator; }
        public Quantity getDenominator() { return denominator; }
        public void setDenominator(Quantity denominator) { this.denominator = denominator; }
    }

    public static class Quantity {
        private Double value;
        private String unit;
        private String system;
        private String code;

        public Quantity() {}
        public Quantity(Double value, String unit, String system, String code) {
            this.value = value; this.unit = unit; this.system = system; this.code = code;
        }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class Amount {
        private Quantity numerator;
        private Quantity denominator;

        public Quantity getNumerator() { return numerator; }
        public void setNumerator(Quantity numerator) { this.numerator = numerator; }
        public Quantity getDenominator() { return denominator; }
        public void setDenominator(Quantity denominator) { this.denominator = denominator; }
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