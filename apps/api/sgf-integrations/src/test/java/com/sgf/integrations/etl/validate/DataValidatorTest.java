package com.sgf.integrations.etl.validate;

import com.sgf.integrations.etl.LegacyProductRecord;
import com.sgf.integrations.etl.transform.DataTransformer.TransformResult;
import com.sgf.integrations.etl.validate.DataValidator.FailedRecord;
import com.sgf.integrations.etl.validate.DataValidator.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataValidator.
 * 
 * Tests cover:
 * - GTIN validation (required, 13-14 digits)
 * - Commercial name validation (required, max length)
 * - Expiry date validation (expired, >5 years past)
 * - Stock validation (non-negative)
 * - Price validation (positive)
 * - CUIT format and checksum validation
 * - Active ingredient validation
 * - Pass rate calculation
 */
class DataValidatorTest {

    private DataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DataValidator();
    }

    @Test
    @DisplayName("Should pass validation for complete valid record")
    void testValidRecordPasses() {
        List<TransformResult> records = List.of(createTransformResult(createValidRecord()));
        
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertTrue(report.allPassed());
        assertEquals(100.0, report.passRate());
    }

    @Test
    @DisplayName("Should fail validation when GTIN is missing")
    void testMissingGtinFails() {
        LegacyProductRecord record = createValidRecord();
        record.setGtin(null);
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(0, report.passed());
        assertEquals(1, report.failed());
        assertFalse(report.allPassed());
        assertTrue(report.failedRecords().get(0).errors().stream()
                .anyMatch(e -> e.contains("GTIN vacío")));
    }

    @Test
    @DisplayName("Should fail validation when GTIN has invalid length")
    void testInvalidGtinLengthFails() {
        LegacyProductRecord record = createValidRecord();
        record.setGtin("12345"); // Too short
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(0, report.passed());
        assertEquals(1, report.failed());
        assertTrue(report.failedRecords().get(0).errors().stream()
                .anyMatch(e -> e.contains("GTIN inválido")));
    }

    @Test
    @DisplayName("Should fail validation when commercial name is empty")
    void testEmptyCommercialNameFails() {
        LegacyProductRecord record = createValidRecord();
        record.setCommercialName(null);
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(0, report.passed());
        assertEquals(1, report.failed());
        assertTrue(report.failedRecords().get(0).errors().stream()
                .anyMatch(e -> e.contains("Nombre comercial vacío")));
    }

    @Test
    @DisplayName("Should warn when commercial name exceeds 200 characters")
    void testLongCommercialNameWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setCommercialName("A".repeat(250));
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
    }

    @Test
    @DisplayName("Should warn when active ingredient is missing")
    void testMissingActiveIngredientWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setActiveIngredient(null);
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
        assertTrue(report.summary().contains("advertencias"));
    }

    @Test
    @DisplayName("Should fail validation when pharmaceutical form is missing")
    void testMissingPharmaceuticalFormFails() {
        LegacyProductRecord record = createValidRecord();
        record.setPharmaceuticalForm(null);
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(0, report.passed());
        assertEquals(1, report.failed());
        assertTrue(report.failedRecords().get(0).errors().stream()
                .anyMatch(e -> e.contains("Forma farmacéutica")));
    }

    @Test
    @DisplayName("Should warn when concentration is missing")
    void testMissingConcentrationWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setConcentration(null);
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
    }

    @Test
    @DisplayName("Should warn when product is expired")
    void testExpiredProductWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setExpiryDate(LocalDate.now().minusDays(30));
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
        assertTrue(report.failedRecords().isEmpty());
    }

    @Test
    @DisplayName("Should fail when product expired more than 5 years ago")
    void testVeryOldExpiryFails() {
        LegacyProductRecord record = createValidRecord();
        record.setExpiryDate(LocalDate.now().minusYears(6));
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(0, report.passed());
        assertEquals(1, report.failed());
        assertTrue(report.failedRecords().get(0).errors().stream()
                .anyMatch(e -> e.contains("vencido hace más de 5 años")));
    }

    @Test
    @DisplayName("Should error when stock is negative")
    void testNegativeStockErrors() {
        LegacyProductRecord record = createValidRecord();
        record.setCurrentStock(-10);
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(0, report.passed());
        assertEquals(1, report.failed());
        assertTrue(report.failedRecords().get(0).errors().stream()
                .anyMatch(e -> e.contains("Stock negativo")));
    }

    @Test
    @DisplayName("Should warn when unit cost is zero or negative")
    void testZeroCostWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setUnitCost(BigDecimal.ZERO);
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
    }

    @Test
    @DisplayName("Should warn when retail price is zero or negative")
    void testZeroPriceWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setRetailPrice(new BigDecimal("-100.00"));
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
    }

    @Test
    @DisplayName("Should warn when CUIT format is invalid")
    void testInvalidCuitFormatWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setSupplierCuit("123456789"); // Invalid format
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
        assertTrue(report.failedRecords().get(0).errors().isEmpty());
    }

    @Test
    @DisplayName("Should warn when CUIT checksum is invalid")
    void testInvalidCuitChecksumWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setSupplierCuit("30-12345678-0"); // Invalid check digit (should be 9)
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
    }

    @Test
    @DisplayName("Should accept valid CUIT with correct checksum")
    void testValidCuitChecksum() {
        LegacyProductRecord record = createValidRecord();
        record.setSupplierCuit("30-12345678-9"); // Valid
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(0, report.warnings());
    }

    @Test
    @DisplayName("Should error when active ingredient is too short")
    void testShortActiveIngredientErrors() {
        LegacyProductRecord record = createValidRecord();
        record.setActiveIngredient("A");
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(0, report.passed());
        assertEquals(1, report.failed());
        assertTrue(report.failedRecords().get(0).errors().stream()
                .anyMatch(e -> e.contains("Principio activo demasiado corto")));
    }

    @Test
    @DisplayName("Should warn when active ingredient contains suspicious numbers")
    void testActiveIngredientWithNumbersWarns() {
        LegacyProductRecord record = createValidRecord();
        record.setActiveIngredient("COMPUESTO 12345");
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        assertEquals(1, report.passed());
        assertEquals(0, report.failed());
        assertEquals(1, report.warnings());
    }

    @Test
    @DisplayName("Should validate batch of records with mixed results")
    void testBatchValidation() {
        List<TransformResult> records = new ArrayList<>();
        
        // Valid record
        records.add(createTransformResult(createValidRecord()));
        
        // Record with missing GTIN
        LegacyProductRecord noGtin = createValidRecord();
        noGtin.setGtin(null);
        records.add(createTransformResult(noGtin));
        
        // Record with warning only
        LegacyProductRecord warning = createValidRecord();
        warning.setActiveIngredient(null);
        records.add(createTransformResult(warning));
        
        ValidationReport report = validator.validate(records);
        
        assertEquals(2, report.passed());
        assertEquals(1, report.failed());
        assertEquals(1, report.warnings());
        assertEquals(66.7, report.passRate(), 0.1);
    }

    @Test
    @DisplayName("Should return failed record details with errors")
    void testFailedRecordDetails() {
        LegacyProductRecord record = createValidRecord();
        record.setGtin(null);
        record.setCommercialName("Producto Fallido");
        
        List<TransformResult> records = List.of(createTransformResult(record));
        ValidationReport report = validator.validate(records);
        
        FailedRecord failed = report.failedRecords().get(0);
        assertEquals("Producto Fallido", failed.productName());
        assertFalse(failed.errors().isEmpty());
        assertNotNull(failed.record());
    }

    // Helper methods
    private LegacyProductRecord createValidRecord() {
        LegacyProductRecord record = new LegacyProductRecord();
        record.setLegacyId("TEST-001");
        record.setGtin("7791234000010");
        record.setCommercialName("Producto Test");
        record.setActiveIngredient("Principio Activo");
        record.setConcentration("500mg");
        record.setPharmaceuticalForm("COMPRIMIDOS");
        record.setCurrentStock(100);
        record.setUnitCost(new BigDecimal("1000.00"));
        record.setRetailPrice(new BigDecimal("1500.00"));
        record.setExpiryDate(LocalDate.now().plusYears(2));
        record.setSupplierCuit("30-12345678-9");
        return record;
    }

    private TransformResult createTransformResult(LegacyProductRecord record) {
        return new TransformResult(record, "CLEAN", List.of());
    }
}
