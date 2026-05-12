package com.sgf.catalog.web;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.service.ProductPriceQuote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String gtin,
        String sku,
        String commercialName,
        String brand,
        String activeIngredient,
        String laboratory,
        String laboratoryCode,
        String snomedCode,
        String troquel,
        String barcode,
        String source,
        String sourceRecordKey,
        LocalDate sourceUpdatedAt,
        BigDecimal latestRetailPrice,
        BigDecimal latestPamiAffiliatePrice,
        Integer latestPamiDiscountCode,
        String latestPamiDiscountLabel,
        String latestPriceSource,
        LocalDate latestPriceEffectiveDate,
        boolean prescriptionRequired,
        boolean requiresTraceability,
        String anmatCategory,
        String presentationDescription,
        String concentration,
        String form,
        Integer unitsPerPackage
) {
    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, ProductPriceQuote priceQuote) {
        var presentation = product.getPresentations().isEmpty() ? null : product.getPresentations().getFirst();
        return new ProductResponse(
                product.getId(),
                product.getGtin(),
                product.getSku(),
                product.getCommercialName(),
                product.getBrand(),
                product.getActiveIngredient(),
                product.getLaboratory(),
                product.getLaboratoryCode(),
                product.getSnomedCode(),
                product.getTroquel(),
                product.getBarcode(),
                product.getSource(),
                product.getSourceRecordKey(),
                product.getSourceUpdatedAt(),
                priceQuote != null ? priceQuote.retailPrice() : null,
                priceQuote != null ? priceQuote.pamiAffiliatePrice() : null,
                priceQuote != null ? priceQuote.pamiDiscountCode() : null,
                priceQuote != null ? priceQuote.pamiDiscountLabel() : null,
                priceQuote != null ? priceQuote.source() : null,
                priceQuote != null ? priceQuote.effectiveDate() : null,
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
