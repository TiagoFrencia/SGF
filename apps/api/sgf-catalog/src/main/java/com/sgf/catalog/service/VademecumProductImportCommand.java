package com.sgf.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VademecumProductImportCommand(
        String source,
        String sourceRecordKey,
        String gtin,
        String troquel,
        String barcode,
        String commercialName,
        String presentation,
        String laboratory,
        String laboratoryCode,
        String activeIngredient,
        String snomedCode,
        String saleCondition,
        String pharmaceuticalForm,
        Integer unitsPerPackage,
        BigDecimal retailPrice,
        BigDecimal pamiAffiliatePrice,
        Integer pamiDiscountCode,
        String pamiDiscountLabel,
        LocalDate effectiveDate
) {
}
