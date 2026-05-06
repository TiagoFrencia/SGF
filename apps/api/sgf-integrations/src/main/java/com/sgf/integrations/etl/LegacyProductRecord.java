package com.sgf.integrations.etl;

import com.sgf.catalog.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a single record extracted from a legacy system.
 * Generic DTO that all extractors (FarmaWin, Nixfarma, DBF) populate.
 *
 * Fields are nullable because legacy systems have inconsistent data quality.
 * Transform step handles cleansing and normalization.
 */
public class LegacyProductRecord {

    // --- Identification ---
    private String legacyId;
    private String gtin;
    private String internalCode;
    private String sku;
    private String commercialName;
    private String brand;

    // --- Pharmacological ---
    private String activeIngredient;
    private String concentration;
    private String pharmaceuticalForm;
    private String presentation;
    private String therapeuticCategory;
    private boolean prescriptionRequired;
    private boolean requiresTraceability;
    private String anmatCategory;

    // --- Pricing ---
    private BigDecimal unitCost;
    private BigDecimal retailPrice;
    private BigDecimal pamiPrice;

    // --- Stock ---
    private Integer currentStock;
    private String lotNumber;
    private LocalDate expiryDate;

    // --- Supplier ---
    private String supplierName;
    private String supplierCuit;
    private BigDecimal lastPurchasePrice;

    // --- Metadata ---
    private String sourceSystem;
    private String sourceTable;
    private String sourceRowId;
    private LocalDate lastModified;

    // --- Validation ---
    private List<String> validationErrors;
    private boolean validated;
    private boolean importReady;

    public LegacyProductRecord() {}

    // Builder pattern for ergonomic construction
    public static LegacyProductRecord fromFarmaWin(Object row) {
        return new LegacyProductRecord(); // implemented in FarmaWinExtractor
    }

    public static LegacyProductRecord fromNixfarma(Object row) {
        return new LegacyProductRecord(); // implemented in NixfarmaExtractor
    }

    public static LegacyProductRecord fromDbf(Object row) {
        return new LegacyProductRecord(); // implemented in DbfExtractor
    }

    // Getters and setters

    public String getLegacyId() { return legacyId; }
    public void setLegacyId(String legacyId) { this.legacyId = legacyId; }

    public String getGtin() { return gtin; }
    public void setGtin(String gtin) { this.gtin = gtin; }

    public String getInternalCode() { return internalCode; }
    public void setInternalCode(String internalCode) { this.internalCode = internalCode; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCommercialName() { return commercialName; }
    public void setCommercialName(String commercialName) { this.commercialName = commercialName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getActiveIngredient() { return activeIngredient; }
    public void setActiveIngredient(String activeIngredient) { this.activeIngredient = activeIngredient; }

    public String getConcentration() { return concentration; }
    public void setConcentration(String concentration) { this.concentration = concentration; }

    public String getPharmaceuticalForm() { return pharmaceuticalForm; }
    public void setPharmaceuticalForm(String pharmaceuticalForm) { this.pharmaceuticalForm = pharmaceuticalForm; }

    public String getPresentation() { return presentation; }
    public void setPresentation(String presentation) { this.presentation = presentation; }

    public String getTherapeuticCategory() { return therapeuticCategory; }
    public void setTherapeuticCategory(String therapeuticCategory) { this.therapeuticCategory = therapeuticCategory; }

    public boolean isPrescriptionRequired() { return prescriptionRequired; }
    public void setPrescriptionRequired(boolean prescriptionRequired) { this.prescriptionRequired = prescriptionRequired; }

    public boolean isRequiresTraceability() { return requiresTraceability; }
    public void setRequiresTraceability(boolean requiresTraceability) { this.requiresTraceability = requiresTraceability; }

    public String getAnmatCategory() { return anmatCategory; }
    public void setAnmatCategory(String anmatCategory) { this.anmatCategory = anmatCategory; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getRetailPrice() { return retailPrice; }
    public void setRetailPrice(BigDecimal retailPrice) { this.retailPrice = retailPrice; }

    public BigDecimal getPamiPrice() { return pamiPrice; }
    public void setPamiPrice(BigDecimal pamiPrice) { this.pamiPrice = pamiPrice; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSupplierCuit() { return supplierCuit; }
    public void setSupplierCuit(String supplierCuit) { this.supplierCuit = supplierCuit; }

    public BigDecimal getLastPurchasePrice() { return lastPurchasePrice; }
    public void setLastPurchasePrice(BigDecimal lastPurchasePrice) { this.lastPurchasePrice = lastPurchasePrice; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }

    public String getSourceRowId() { return sourceRowId; }
    public void setSourceRowId(String sourceRowId) { this.sourceRowId = sourceRowId; }

    public LocalDate getLastModified() { return lastModified; }
    public void setLastModified(LocalDate lastModified) { this.lastModified = lastModified; }

    public List<String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }

    public boolean isValidated() { return validated; }
    public void setValidated(boolean validated) { this.validated = validated; }

    public boolean isImportReady() { return importReady; }
    public void setImportReady(boolean importReady) { this.importReady = importReady; }
}