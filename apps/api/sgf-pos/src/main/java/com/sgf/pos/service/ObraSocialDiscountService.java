package com.sgf.pos.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Applies obra social (health insurance) discounts at the POS terminal.
 *
 * In Argentina, obras sociales and prepagas provide coverage for medications:
 * - PAMI (national retirement/pension fund): covers up to 100% for certain drugs
 * - OSDE, Swiss Medical, etc.: percentage or fixed co-payment schemes
 * - Obras sociales provinciales (IAPOS, DOSEP, etc.)
 *
 * The actual validation is delegated to ADESFA (via AdesfaGateway).
 * This service calculates discount application and co-payment amounts
 * at the POS layer for immediate display to the cashier and patient.
 */
@Service
public class ObraSocialDiscountService {

    private static final Logger log = LoggerFactory.getLogger(ObraSocialDiscountService.class);

    /**
     * Standard obra social codes (in production, this comes from the ADESFA validator response).
     */
    public static final String PAMI = "PAMI";
    public static final String OSDE = "OSDE";
    public static final String SWISS_MEDICAL = "SWISS_MEDICAL";
    public static final String GALENO = "GALENO";
    public static final String OMINT = "OMINT";
    public static final String IAPOS = "IAPOS";
    public static final String DOSEP = "DOSEP";
    public static final String APROSS = "APROSS";
    public static final String UNION_PERSONAL = "UNION_PERSONAL";
    public static final String ACA_SALUD = "ACA_SALUD";

    /**
     * Calculate discount for a list of items given an obra social code and patient info.
     *
     * In production this would call the ADESFA gateway for each item.
     * For now, it provides the discount structure with known coverage rules.
     *
     * @param obraSocialCode  The obra social/financiador code (e.g., "PAMI")
     * @param patientDocument Patient's DNI/CUIT to validate affiliation
     * @param items           Cart items with product IDs and prices
     * @return DiscountResult with per-item coverage breakdown and totals
     */
    public DiscountResult calculate(String obraSocialCode, String patientDocument,
                                     List<DiscountItem> items) {
        log.info("Calculating OS discount for {} (patient: {})", obraSocialCode, patientDocument);

        List<DiscountedLine> lines = new ArrayList<>();
        BigDecimal totalBefore = BigDecimal.ZERO;
        BigDecimal totalCovered = BigDecimal.ZERO;
        BigDecimal totalPatient = BigDecimal.ZERO;

        for (DiscountItem item : items) {
            // Simulated coverage rules per obra social
            CoverageRule rule = getCoverageRule(obraSocialCode);

            BigDecimal before = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            totalBefore = totalBefore.add(before);

            BigDecimal patientPay;
            BigDecimal covered;

            if (rule.coverageType() == CoverageType.PERCENTAGE) {
                // e.g., PAMI covers 70% → patient pays 30%
                covered = before.multiply(rule.coverageRate());
                patientPay = before.subtract(covered);
            } else {
                // Fixed amount per item
                covered = rule.fixedAmount().multiply(BigDecimal.valueOf(item.quantity()));
                patientPay = before.subtract(covered);
                if (patientPay.compareTo(BigDecimal.ZERO) < 0) {
                    patientPay = BigDecimal.ZERO;
                    covered = before;
                }
            }

            totalCovered = totalCovered.add(covered);
            totalPatient = totalPatient.add(patientPay);

            lines.add(new DiscountedLine(
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    before,
                    covered,
                    patientPay,
                    item.unitPrice(),
                    rule.coverageType()
            ));
        }

        return new DiscountResult(
                obraSocialCode, patientDocument,
                lines, totalBefore, totalCovered, totalPatient,
                totalPatient.compareTo(BigDecimal.ZERO) > 0
        );
    }

    /**
     * Quick coverage check for a single product (for scanning feedback).
     */
    public QuickCoverage quickCheck(String obraSocialCode, String productGtin) {
        CoverageRule rule = getCoverageRule(obraSocialCode);
        return new QuickCoverage(
                obraSocialCode,
                productGtin,
                rule.coverageType(),
                rule.coverageRate(),
                rule.fixedAmount()
        );
    }

    /**
     * Get coverage rule for a specific obra social.
     * In production, this would query ADESFA or a local cache of agreements.
     */
    private CoverageRule getCoverageRule(String obraSocialCode) {
        if (obraSocialCode == null) {
            return CoverageRule.NO_COVERAGE;
        }
        return switch (obraSocialCode.toUpperCase()) {
            case PAMI -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.70"), null, "PAMI — 70% cobertura ambulatoria");
            case OSDE -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.40"), null, "OSDE — 40% cobertura");
            case SWISS_MEDICAL -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.50"), null, "Swiss Medical — 50% cobertura");
            case GALENO -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.40"), null, "Galeno — 40% cobertura");
            case OMINT -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.50"), null, "OMINT — 50% cobertura");
            case IAPOS -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.40"), null, "IAPOS — 40% cobertura");
            case DOSEP -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.40"), null, "DOSEP — 40% cobertura");
            case APROSS -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.50"), null, "APROSS — 50% cobertura");
            case UNION_PERSONAL -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.40"), null, "Unión Personal — 40% cobertura");
            case ACA_SALUD -> new CoverageRule(CoverageType.PERCENTAGE,
                    new BigDecimal("0.40"), null, "ACA Salud — 40% cobertura");
            default -> CoverageRule.NO_COVERAGE;
        };
    }

    // --- Types ---

    public enum CoverageType {
        PERCENTAGE,  // e.g., 70% covered by obra social
        FIXED        // e.g., $500 per item covered
    }

    public record CoverageRule(
            CoverageType coverageType,
            BigDecimal coverageRate,     // 0.0 to 1.0 for PERCENTAGE
            BigDecimal fixedAmount,       // For FIXED type
            String description
    ) {
        public static final CoverageRule NO_COVERAGE = new CoverageRule(
                CoverageType.PERCENTAGE, BigDecimal.ZERO, null,
                "Sin cobertura"
        );
    }

    /**
     * Item to discount (input).
     */
    public record DiscountItem(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}

    /**
     * Single line after discount applied.
     */
    public record DiscountedLine(
            String productId,
            String productName,
            int quantity,
            BigDecimal priceBefore,
            BigDecimal covered,
            BigDecimal patientPays,
            BigDecimal unitPrice,
            CoverageType coverageType
    ) {}

    /**
     * Full discount calculation result.
     */
    public record DiscountResult(
            String obraSocialCode,
            String patientDocument,
            List<DiscountedLine> lines,
            BigDecimal totalBefore,
            BigDecimal totalCovered,
            BigDecimal totalPatientPays,
            boolean hasCoverage
    ) {
        public BigDecimal coveragePercentage() {
            if (totalBefore.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
            return totalCovered.divide(totalBefore, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    /**
     * Quick coverage check result.
     */
    public record QuickCoverage(
            String obraSocialCode,
            String productGtin,
            CoverageType coverageType,
            BigDecimal coverageRate,
            BigDecimal fixedAmount
    ) {
        public boolean isCovered() {
            return coverageRate != null && coverageRate.compareTo(BigDecimal.ZERO) > 0;
        }

        public String summary() {
            if (!isCovered()) return "Sin cobertura";
            if (coverageType == CoverageType.PERCENTAGE) {
                return coverageRate.multiply(BigDecimal.valueOf(100)).intValue() + "% cubierto";
            }
            return "$" + fixedAmount + " cubierto";
        }
    }
}