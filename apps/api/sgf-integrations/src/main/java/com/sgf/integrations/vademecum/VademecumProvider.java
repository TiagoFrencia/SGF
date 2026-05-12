package com.sgf.integrations.vademecum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VademecumProvider {
    String providerCode();
    List<VademecumProduct> search(String searchData);
    Optional<LocalDate> currentVigencia();

    record VademecumProduct(
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
}
