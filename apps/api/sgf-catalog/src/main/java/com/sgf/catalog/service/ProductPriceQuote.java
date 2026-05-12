package com.sgf.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductPriceQuote(
        BigDecimal retailPrice,
        BigDecimal pamiAffiliatePrice,
        Integer pamiDiscountCode,
        String pamiDiscountLabel,
        String source,
        LocalDate effectiveDate
) {
}
