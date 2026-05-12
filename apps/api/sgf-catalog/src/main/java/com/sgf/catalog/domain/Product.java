package com.sgf.catalog.domain;

import com.sgf.core.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true, length = 14)
    private String gtin;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String commercialName;

    private String brand;
    private String activeIngredient;
    private String laboratory;
    private String laboratoryCode;
    private String snomedCode;
    private String troquel;
    private String barcode;
    private String source;
    private String sourceRecordKey;
    private LocalDate sourceUpdatedAt;

    @Column(nullable = false)
    private boolean prescriptionRequired;

    @Column(nullable = false)
    private boolean requiresTraceability;

    private String anmatCategory;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductPresentation> presentations = new ArrayList<>();

    public String getGtin() {
        return gtin;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public void setCommercialName(String commercialName) {
        this.commercialName = commercialName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public String getLaboratory() {
        return laboratory;
    }

    public void setLaboratory(String laboratory) {
        this.laboratory = laboratory;
    }

    public String getLaboratoryCode() {
        return laboratoryCode;
    }

    public void setLaboratoryCode(String laboratoryCode) {
        this.laboratoryCode = laboratoryCode;
    }

    public String getSnomedCode() {
        return snomedCode;
    }

    public void setSnomedCode(String snomedCode) {
        this.snomedCode = snomedCode;
    }

    public String getTroquel() {
        return troquel;
    }

    public void setTroquel(String troquel) {
        this.troquel = troquel;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceRecordKey() {
        return sourceRecordKey;
    }

    public void setSourceRecordKey(String sourceRecordKey) {
        this.sourceRecordKey = sourceRecordKey;
    }

    public LocalDate getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public void setSourceUpdatedAt(LocalDate sourceUpdatedAt) {
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public boolean isPrescriptionRequired() {
        return prescriptionRequired;
    }

    public void setPrescriptionRequired(boolean prescriptionRequired) {
        this.prescriptionRequired = prescriptionRequired;
    }

    public boolean isRequiresTraceability() {
        return requiresTraceability;
    }

    public void setRequiresTraceability(boolean requiresTraceability) {
        this.requiresTraceability = requiresTraceability;
    }

    public String getAnmatCategory() {
        return anmatCategory;
    }

    public void setAnmatCategory(String anmatCategory) {
        this.anmatCategory = anmatCategory;
    }

    public List<ProductPresentation> getPresentations() {
        return presentations;
    }

    public void setPresentations(List<ProductPresentation> presentations) {
        this.presentations = presentations;
    }

    private String alfabetCode;
    private String kairosCode;

    public String getAlfabetCode() {
        return alfabetCode;
    }

    public void setAlfabetCode(String alfabetCode) {
        this.alfabetCode = alfabetCode;
    }

    public String getKairosCode() {
        return kairosCode;
    }

    public void setKairosCode(String kairosCode) {
        this.kairosCode = kairosCode;
    }
}
