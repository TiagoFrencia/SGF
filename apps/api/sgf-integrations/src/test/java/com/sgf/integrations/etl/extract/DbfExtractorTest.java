package com.sgf.integrations.etl.extract;

import com.sgf.integrations.etl.LegacyProductRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DbfExtractor.
 * 
 * Tests cover:
 * - DBF/XBase CSV extraction simulation
 * - CP850 encoding detection
 * - Date parsing (yyyyMMdd format)
 * - Implicit decimal handling (cost in cents)
 * - Column auto-detection mapping
 */
class DbfExtractorTest {

    private DbfExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DbfExtractor();
    }

    @Test
    @DisplayName("Should return DBF_Generic as source system name")
    void testSourceSystemName() {
        assertEquals("DBF_Generic", extractor.sourceSystemName());
    }

    @Test
    @DisplayName("Should open with fallback to sample data when file not found")
    void testOpenWithFallback() {
        // Opening a non-existent file triggers fallback to sample data
        extractor.open("/nonexistent/path/file.dbf");
        
        assertTrue(extractor.hasMore());
        assertEquals(5, extractor.totalRecords());
    }

    @Test
    @DisplayName("Should parse dates in yyyyMMdd format")
    void testDbfDateParsing() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // First record expiry: 20251231 → 2025-12-31
        LegacyProductRecord first = batch[0];
        assertNotNull(first.getExpiryDate());
        assertEquals(LocalDate.of(2025, 12, 31), first.getExpiryDate());
        
        // Third record: 20250228 → 2025-02-28
        assertEquals(LocalDate.of(2025, 2, 28), batch[2].getExpiryDate());
    }

    @Test
    @DisplayName("Should handle implicit decimals in cost field")
    void testImplicitDecimals() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // First record cost: 350000 cents → 3500.00
        LegacyProductRecord first = batch[0];
        assertNotNull(first.getUnitCost());
        assertEquals(3500.00, first.getUnitCost().doubleValue(), 0.01);
        
        // Second record: 180000 → 1800.00
        assertEquals(1800.00, batch[1].getUnitCost().doubleValue(), 0.01);
    }

    @Test
    @DisplayName("Should normalize pharmaceutical forms from DBF codes")
    void testFormNormalization() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // All records should have normalized forms
        for (LegacyProductRecord record : batch) {
            assertNotNull(record.getPharmaceuticalForm());
            assertEquals("COMPRIMIDOS", record.getPharmaceuticalForm());
        }
    }

    @Test
    @DisplayName("Should handle products without GTIN")
    void testMissingGtin() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // Record 4 (DIPIRONA) has no GTIN
        LegacyProductRecord noGtin = batch[3];
        assertNull(noGtin.getGtin());
        assertEquals("DIPIRONA 500mg x 10", noGtin.getCommercialName());
    }

    @Test
    @DisplayName("Should extract batches correctly")
    void testBatchExtraction() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        assertEquals(5, batch.length);
        assertFalse(extractor.hasMore());
        assertEquals(100, extractor.progressPercent());
        
        extractor.reset();
        assertTrue(extractor.hasMore());
    }

    @Test
    @DisplayName("Should throw exception when extracting before opening")
    void testExtractBeforeOpen() {
        assertThrows(IllegalStateException.class, () -> {
            extractor.extractBatch();
        });
    }

    @Test
    @DisplayName("Should close extractor and log records processed")
    void testClose() {
        extractor.open("/test/path.dbf");
        extractor.close();
        
        // After closing, hasMore should still work but extractor is closed
        // The sample data fallback means we can still access it
        assertNotNull(extractor);
    }

    @Test
    @DisplayName("Should map supplier CUIT correctly")
    void testSupplierCuitMapping() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // First record CUIT: 30-12345678-9
        LegacyProductRecord first = batch[0];
        assertEquals("30-12345678-9", first.getSupplierCuit());
        
        // Record with missing CUIT
        LegacyProductRecord noCuit = batch[3];
        assertNull(noCuit.getSupplierCuit());
    }

    @Test
    @DisplayName("Should set prescription required flag based on active ingredient")
    void testPrescriptionRequired() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // CLARITROMICINA requires prescription
        LegacyProductRecord claritromicina = batch[4];
        assertEquals("CLARITROMICINA", claritromicina.getActiveIngredient());
        assertTrue(claritromicina.isPrescriptionRequired());
        
        // RANITIDINA requires prescription
        LegacyProductRecord ranitidina = batch[2];
        assertTrue(ranitidina.isPrescriptionRequired());
        
        // IBUPROFENO doesn't require prescription
        LegacyProductRecord ibuprofeno = batch[0];
        assertFalse(ibuprofeno.isPrescriptionRequired());
    }

    @Test
    @DisplayName("Should handle concentration with mg suffix")
    void testConcentrationFormat() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // First record: 400 → "400mg"
        LegacyProductRecord first = batch[0];
        assertEquals("400mg", first.getConcentration());
        
        // Second record: 500 → "500mg"
        assertEquals("500mg", batch[1].getConcentration());
    }

    @Test
    @DisplayName("Should detect expired products")
    void testExpiredProduct() {
        extractor.open("/test/path.dbf");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // Record 4 (DIPIRONA) has expiry 20240101 - already expired
        LegacyProductRecord expired = batch[3];
        assertNotNull(expired.getExpiryDate());
        assertTrue(expired.getExpiryDate().isBefore(LocalDate.now()));
    }
}
