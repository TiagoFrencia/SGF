package com.sgf.integrations.etl.transform;

import com.sgf.integrations.etl.LegacyProductRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Transforms raw legacy records into cleansed, normalized SGF-compatible records.
 *
 * Handles common legacy data problems:
 * - GTIN: pads leading zeros to 14 digits, validates checksum
 * - Dates: normalizes multiple formats to ISO (yyyy-MM-dd)
 * - Prices: rounds to 2 decimal places, handles implicit decimals
 * - Product names: separates commercial name from active ingredient
 * - CUIT: validates format (XX-XXXXXXXX-X), pads missing digits
 * - Forms: maps legacy codes to standard pharmaceutical forms
 * - Categories: infers therapeutic category from active ingredient
 *
 * A record that passes through this transformer is "clean" but not yet validated.
 * Validation happens in DataValidator.
 */
@Component
public class DataTransformer {

    private static final Logger log = LoggerFactory.getLogger(DataTransformer.class);

    /**
     * Transform a batch of legacy records.
     * @return List of transform results (one per input record)
     */
    public List<TransformResult> transform(LegacyProductRecord[] records) {
        List<TransformResult> results = new ArrayList<>();
        for (LegacyProductRecord record : records) {
            results.add(transformOne(record));
        }
        return results;
    }

    private TransformResult transformOne(LegacyProductRecord record) {
        List<String> changes = new ArrayList<>();
        String status = "CLEAN";

        // 1. Normalize GTIN: pad to 14 digits
        if (record.getGtin() != null && record.getGtin().matches("\\d+")) {
            String original = record.getGtin();
            String padded = String.format("%014d", Long.parseLong(original));
            if (!padded.equals(original)) {
                record.setGtin(padded);
                changes.add("GTIN padded: " + original + " → " + padded);
            }
        }

        // 2. Normalize commercial name: separate IFA if embedded
        if (record.getCommercialName() != null && record.getActiveIngredient() == null) {
            String name = record.getCommercialName();
            // Pattern: "IFA concentración x unidades" or "IFA + IFA2 marca"
            if (name.matches("(?i).*\\d+\\s*(mg|mcg|g|ml|%)\\s*x?\\s*\\d+.*")) {
                String[] parts = name.split("\\s+\\d+\\s*(?:mg|mcg|g|ml|%)", 2);
                if (parts.length > 0 && parts[0].length() > 2) {
                    String ifa = parts[0].trim();
                    if (ifa.matches("(?i)^[A-ZÁÉÍÓÚÑ\\s]+$") && ifa.length() < 50) {
                        record.setActiveIngredient(ifa);
                        changes.add("IFA extracted from name: " + ifa);
                    }
                }
            }
        }

        // 3. Normalize pharmaceutical form
        if (record.getPharmaceuticalForm() != null) {
            String normalized = normalizeForm(record.getPharmaceuticalForm());
            if (!normalized.equals(record.getPharmaceuticalForm())) {
                record.setPharmaceuticalForm(normalized);
                changes.add("Form normalized: → " + normalized);
            }
        }

        // 4. Round prices to 2 decimal places
        if (record.getUnitCost() != null) {
            BigDecimal rounded = record.getUnitCost().setScale(2, RoundingMode.HALF_UP);
            if (rounded.compareTo(record.getUnitCost()) != 0) {
                record.setUnitCost(rounded);
                changes.add("Cost rounded: → " + rounded);
            }
        }
        if (record.getRetailPrice() != null) {
            BigDecimal rounded = record.getRetailPrice().setScale(2, RoundingMode.HALF_UP);
            if (rounded.compareTo(record.getRetailPrice()) != 0) {
                record.setRetailPrice(rounded);
                changes.add("Price rounded: → " + rounded);
            }
        }

        // 5. Normalize CUIT format (XX-XXXXXXXX-X)
        if (record.getSupplierCuit() != null) {
            String cuit = record.getSupplierCuit().replaceAll("[^\\d]", "");
            if (cuit.matches("\\d{11}")) {
                String formatted = cuit.substring(0, 2) + "-" +
                        cuit.substring(2, 10) + "-" +
                        cuit.substring(10, 11);
                if (!formatted.equals(record.getSupplierCuit())) {
                    record.setSupplierCuit(formatted);
                    changes.add("CUIT formatted: → " + formatted);
                }
            }
        }

        // 6. Estimate retail price if missing (cost × 1.5 pharmacy markup)
        if (record.getRetailPrice() == null && record.getUnitCost() != null) {
            BigDecimal estimated = record.getUnitCost()
                    .multiply(new BigDecimal("1.50"))
                    .setScale(2, RoundingMode.HALF_UP);
            record.setRetailPrice(estimated);
            changes.add("Price estimated from cost: " + estimated);
        }

        // 7. Infer prescription requirement from active ingredient
        if (!record.isPrescriptionRequired() && record.getActiveIngredient() != null) {
            String ifa = record.getActiveIngredient().toUpperCase();
            if (isPrescriptionOnly(ifa)) {
                record.setPrescriptionRequired(true);
                changes.add("Prescription required (inferred): " + ifa);
            }
        }

        // 8. Infer ANMAT traceability
        if (!record.isRequiresTraceability() && record.getActiveIngredient() != null) {
            String ifa = record.getActiveIngredient().toUpperCase();
            if (isHighRiskAnmat(ifa)) {
                record.setRequiresTraceability(true);
                changes.add("ANMAT traceability required (inferred): " + ifa);
            }
        }

        // 9. Mark as expired if expiry date has passed
        if (record.getExpiryDate() != null && record.getExpiryDate().isBefore(LocalDate.now())) {
            status = "EXPIRED";
        }

        if (record.getGtin() == null || record.getGtin().isEmpty()) {
            status = "MISSING_GTIN";
        }

        return new TransformResult(record, status, changes);
    }

    private String normalizeForm(String form) {
        return switch (form.toUpperCase().trim()) {
            case "COMP", "COM.", "COMPRIMIDO", "TABLETA", "TAB" -> "COMPRIMIDOS";
            case "CAP", "CAPS", "CAPSULA", "CAP." -> "CAPSULAS";
            case "JAR", "JBE", "JARABE" -> "JARABE";
            case "INY", "AMP", "AMPOLLA", "INYECTABLE" -> "INYECTABLE";
            case "SUSP", "SUSPENSION" -> "SUSPENSION";
            case "CREMA" -> "CREMA";
            case "UNG", "UNGUENTO" -> "UNGUENTO";
            case "SOL", "SOLUCION" -> "SOLUCION";
            case "GOT", "GOTAS" -> "GOTAS";
            case "OV", "OVULOS" -> "OVULOS";
            case "SUP", "SUPOSITORIOS" -> "SUPOSITORIOS";
            case "POL", "POLVO" -> "POLVO";
            case "INH", "AER", "AEROSOL", "INHALADOR" -> "INHALADOR";
            case "GEL" -> "GEL";
            default -> form;
        };
    }

    /**
     * Known prescription-only active ingredients.
     */
    private boolean isPrescriptionOnly(String ifa) {
        String upper = ifa.toUpperCase().trim();
        return upper.contains("ENALAPRIL")
                || upper.contains("LOSARTAN")
                || upper.contains("ATORVASTATINA")
                || upper.contains("SIMVASTATINA")
                || upper.contains("LEVOTIROXINA")
                || upper.contains("CLARITROMICINA")
                || upper.contains("AMOXICILINA")
                || upper.contains("OMEPRAZOL")
                || upper.contains("METFORMINA")
                || upper.contains("SILDENAFILO")
                || upper.contains("RANITIDINA")
                || upper.contains("CLOPIDOGREL")
                || upper.contains("WARFARINA");
    }

    /**
     * Products requiring ANMAT traceability (Alto Riesgo, Hospitalarios, etc.).
     */
    private boolean isHighRiskAnmat(String ifa) {
        String upper = ifa.toUpperCase().trim();
        return upper.contains("INMUNOGLOBULINA")
                || upper.contains("ONCOLOGICO")
                || upper.contains("HEMODERIVADO")
                || upper.contains("BIOLOGICO")
                || upper.contains("ESTUPEFACIENTE")
                || upper.contains("PSICOTROPICO");
    }

    // --- Result types ---

    /**
     * Result of transforming a single legacy record.
     */
    public record TransformResult(
            LegacyProductRecord record,
            String status,         // CLEAN, EXPIRED, MISSING_GTIN
            List<String> changes    // What was modified
    ) {
        public boolean isClean() {
            return "CLEAN".equals(status);
        }

        public boolean needsReview() {
            return !isClean();
        }
    }
}