package com.sgf.catalog.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateProductRequest(
        @Pattern(regexp = "\\d{14}", message = "GTIN must have 14 digits") String gtin,
        @NotBlank String sku,
        @NotBlank String commercialName,
        String brand,
        String activeIngredient,
        boolean prescriptionRequired,
        boolean requiresTraceability,
        String anmatCategory,
        @NotBlank String presentationDescription,
        String concentration,
        String form,
        @Min(value = 1, message = "unitsPerPackage must be positive") Integer unitsPerPackage
) {
}
