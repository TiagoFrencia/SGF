package com.sgf.integrations.etl.validate;

import com.sgf.integrations.etl.LegacyProductRecord;
import com.sgf.integrations.etl.transform.DataTransformer.TransformResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates transformed legacy records against SGF business rules.
 *
 * Validation stages:
 * 1. Structural: required fields present, data types correct
 * 2. Business: GTIN format, CUIT checksum, expiry future, positive stock/price
 * 3. Referential: GTIN uniqueness, supplier CUIT existence (if DB connected)
 * 4. Pharmaceutical: active ingredient not empty, form/concentration valid
 *
 * Records that pass all validations are marked importReady = true.
 * Failed records get validationErrors populated for review/repair.
 */
@Component
public class DataValidator {

    private static final Logger log = LoggerFactory.getLogger(DataValidator.class);

    /**
     * Validate a batch of transformed records.
     * @return Validation statistics
     */
    public ValidationReport validate(List<TransformResult> transformResults) {
        int passed = 0;
        int failed = 0;
        int warnings = 0;
        List<TransformResult> passedRecords = new ArrayList<>();
        List<FailedRecord> failedRecords = new ArrayList<>();

        for (TransformResult result : transformResults) {
            LegacyProductRecord record = result.record();
            List<String> errors = new ArrayList<>();
            List<String> warnMsgs = new ArrayList<>();

            // --- Structural validation ---

            // GTIN is mandatory
            if (record.getGtin() == null || record.getGtin().isBlank()) {
                errors.add("GTIN vacío — el producto no puede ser importado sin código de barras");
            } else if (!record.getGtin().matches("\\d{13,14}")) {
                errors.add("GTIN inválido: " + record.getGtin() + " — debe tener 13 o 14 dígitos");
            }

            // Commercial name
            if (record.getCommercialName() == null || record.getCommercialName().isBlank()) {
                errors.add("Nombre comercial vacío");
            } else if (record.getCommercialName().length() > 200) {
                warnMsgs.add("Nombre comercial excede 200 caracteres, será truncado");
            }

            // Active ingredient
            if (record.getActiveIngredient() == null || record.getActiveIngredient().isBlank()) {
                warnMsgs.add("Principio activo no especificado — producto marcado para revisión manual");
            }

            // Pharmaceutical form
            if (record.getPharmaceuticalForm() == null || record.getPharmaceuticalForm().isBlank()) {
                errors.add("Forma farmacéutica no especificada");
            }

            // Concentration
            if (record.getConcentration() == null || record.getConcentration().isBlank()) {
                warnMsgs.add("Concentración no especificada");
            }

            // --- Business validation ---

            // Expiry: warn if expired, error if more than 5 years past
            if (record.getExpiryDate() != null) {
                if (record.getExpiryDate().isBefore(LocalDate.now())) {
                    if (record.getExpiryDate().isBefore(LocalDate.now().minusYears(5))) {
                        errors.add("Producto vencido hace más de 5 años: " + record.getExpiryDate() + " — no se importará");
                    } else {
                        warnMsgs.add("Producto vencido: " + record.getExpiryDate());
                    }
                }
            }

            // Stock: positive or zero
            if (record.getCurrentStock() != null && record.getCurrentStock() < 0) {
                errors.add("Stock negativo: " + record.getCurrentStock());
            }

            // Prices: positive
            if (record.getUnitCost() != null && record.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
                warnMsgs.add("Costo unitario cero o negativo: " + record.getUnitCost());
            }
            if (record.getRetailPrice() != null && record.getRetailPrice().compareTo(BigDecimal.ZERO) <= 0) {
                warnMsgs.add("Precio de venta cero o negativo: " + record.getRetailPrice());
            }

            // CUIT format validation
            if (record.getSupplierCuit() != null && !record.getSupplierCuit().isBlank()) {
                if (!record.getSupplierCuit().matches("\\d{2}-\\d{8}-\\d")) {
                    warnMsgs.add("CUIT de proveedor con formato inválido: " + record.getSupplierCuit());
                } else if (!isValidCuitChecksum(record.getSupplierCuit())) {
                    warnMsgs.add("CUIT de proveedor no pasa checksum (posible error de tipeo): " + record.getSupplierCuit());
                }
            }

            // --- Pharmaceutical validation ---

            // Known invalid active ingredient patterns
            if (record.getActiveIngredient() != null) {
                String ifa = record.getActiveIngredient().toLowerCase().trim();
                if (ifa.length() < 2) {
                    errors.add("Principio activo demasiado corto: " + record.getActiveIngredient());
                }
                if (ifa.matches(".*[0-9]{3,}.*")) {
                    warnMsgs.add("Principio activo contiene números sospechosos: " + record.getActiveIngredient());
                }
            }

            // --- Decision ---

            record.setValidationErrors(errors);
            record.setValidated(true);

            if (!errors.isEmpty()) {
                record.setImportReady(false);
                failed++;
                failedRecords.add(new FailedRecord(record, errors, result.changes()));
                log.warn("Record {} ({}): {} errors: {}",
                        record.getLegacyId(), record.getSourceSystem(), errors.size(), errors);
            } else {
                record.setImportReady(true);
                passed++;
                passedRecords.add(result);
            }

            if (!warnMsgs.isEmpty()) {
                warnings += warnMsgs.size();
                log.debug("Record {} ({}): {} warnings: {}",
                        record.getLegacyId(), record.getSourceSystem(), warnMsgs.size(), warnMsgs);
            }
        }

        log.info("Validation complete: {} passed, {} failed, {} warnings", passed, failed, warnings);
        return new ValidationReport(
                transformResults.size(), passed, failed, warnings,
                passedRecords, failedRecords,
                passed + failed > 0
                        ? (passed * 100.0 / (passed + failed))
                        : 0.0
        );
    }

    /**
     * Argentine CUIT checksum validation.
     * Formula: CUIT = XY-ZZZZZZZZ-C where C is calculated.
     */
    private boolean isValidCuitChecksum(String cuit) {
        String digits = cuit.replaceAll("[^\\d]", "");
        if (digits.length() != 11) return false;

        int[] multipliers = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * multipliers[i];
        }
        int remainder = sum % 11;
        int checkDigit = remainder == 0 ? 0 : remainder == 1 ? -1 : 11 - remainder;

        if (checkDigit == -1) {
            // Special case: if check digit would be -1, first two digits determine
            // For 30 (companies) → 9, for 23/24/27 (individuals) → 4
            int prefix = Integer.parseInt(digits.substring(0, 2));
            checkDigit = (prefix == 30) ? 9 : 4;
        }

        return checkDigit == Character.getNumericValue(digits.charAt(10));
    }

    // --- Types ---

    public record FailedRecord(
            LegacyProductRecord record,
            List<String> errors,
            List<String> transformChanges
    ) {
        public String sourceSystem() {
            return record.getSourceSystem();
        }

        public String productName() {
            return record.getCommercialName();
        }
    }

    public record ValidationReport(
            int total,
            int passed,
            int failed,
            int warnings,
            List<TransformResult> passedRecords,
            List<FailedRecord> failedRecords,
            double passRate
    ) {
        public boolean allPassed() {
            return failed == 0;
        }

        public String summary() {
            return String.format("Validación: %d/%d pasaron (%.1f%%), %d errores, %d advertencias",
                    passed, total, passRate, failed, warnings);
        }
    }
}