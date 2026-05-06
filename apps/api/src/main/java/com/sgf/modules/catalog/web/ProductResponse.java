package com.sgf.modules.catalog.web;

import com.sgf.modules.catalog.domain.Product;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String gtin,
        String sku,
        String commercialName,
        String brand,
        String activeIngredient,
        boolean prescriptionRequired,
        boolean requiresTraceability,
        String anmatCategory,
        String presentationDescription,
        String concentration,
        String form,
        Integer unitsPerPackage
) {
    public static ProductResponse from(Product product) {
        var presentation = product.getPresentations().isEmpty() ? null : product.getPresentations().getFirst();
        return new ProductResponse(
                product.getId(),
                product.getGtin(),
                product.getSku(),
                product.getCommercialName(),
                product.getBrand(),
                product.getActiveIngredient(),
                product.isPrescriptionRequired(),
                product.isRequiresTraceability(),
                product.getAnmatCategory(),
                presentation != null ? presentation.getDescription() : null,
                presentation != null ? presentation.getConcentration() : null,
                presentation != null ? presentation.getForm() : null,
                presentation != null ? presentation.getUnitsPerPackage() : null
        );
    }
}
