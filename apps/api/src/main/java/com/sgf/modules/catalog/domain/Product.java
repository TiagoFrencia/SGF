package com.sgf.modules.catalog.domain;

import com.sgf.modules.core.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
}
