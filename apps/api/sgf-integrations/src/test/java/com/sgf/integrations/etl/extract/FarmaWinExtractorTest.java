package com.sgf.integrations.etl.extract;

import com.sgf.integrations.etl.LegacyProductRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FarmaWinExtractor.
 * 
 * Tests cover:
 * - Firebird/SQL Server extraction simulation
 * - Pharmaceutical form normalization
 * - Date parsing (dd/MM/yyyy format)
 * - GTIN padding (leading zeros)
 * - Batch extraction
 */
class FarmaWinExtractorTest {

    private FarmaWinExtractor extractor;
    private static final DateTimeFormatter FARMAWIN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @BeforeEach
    void setUp() {
        extractor = new FarmaWinExtractor();
    }

    @Test
    @DisplayName("Should return FarmaWin as source system name")
    void testSourceSystemName() {
        assertEquals("FarmaWin", extractor.sourceSystemName());
    }

    @Test
    @DisplayName("Should open extractor and load sample records")
    void testOpenLoadsRecords() {
        extractor.open("jdbc:firebird:localhost/sample.fdb");
        
        assertTrue(extractor.hasMore());
        assertEquals(5, extractor.totalRecords());
        assertEquals(0, extractor.progressPercent());
    }

    @Test
    @DisplayName("Should normalize pharmaceutical forms from legacy codes")
    void testFormNormalization() {
        extractor.open("test");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // Verify all records have normalized forms
        for (LegacyProductRecord record : batch) {
            assertNotNull(record.getPharmaceuticalForm());
            assertNotEquals("COMP", record.getPharmaceuticalForm());
            assertNotEquals("COM.", record.getPharmaceuticalForm());
            assertEquals("COMPRIMIDOS", record.getPharmaceuticalForm());
        }
    }

    @Test
    @DisplayName("Should parse dates in dd/MM/yyyy format")
    void testDateParsing() {
        extractor.open("test");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // First record should have expiry 31/12/2025
        LegacyProductRecord first = batch[0];
        assertNotNull(first.getExpiryDate());
        assertEquals(LocalDate.of(2025, 12, 31), first.getExpiryDate());
        
        // Second record: 15/06/2025
        assertEquals(LocalDate.of(2025, 6, 15), batch[1].getExpiryDate());
    }

    @Test
    @DisplayName("Should handle expired products")
    void testExpiredProduct() {
        extractor.open("test");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // Record 4 (ATENOLOL) has expiry 10/10/2024 - already expired
        LegacyProductRecord expired = batch[3];
        assertNotNull(expired.getExpiryDate());
        assertTrue(expired.getExpiryDate().isBefore(LocalDate.now()));
        assertNull(expired.getGtin()); // Missing GTIN in test data
    }

    @Test
    @DisplayName("Should extract batches correctly")
    void testBatchExtraction() {
        extractor.open("test");
        
        // Extract first batch
        LegacyProductRecord[] batch1 = extractor.extractBatch();
        assertEquals(5, batch1.length); // All records fit in one batch (BATCH_SIZE=100)
        assertFalse(extractor.hasMore());
        assertEquals(100, extractor.progressPercent());
        
        // Reset and extract again
        extractor.reset();
        assertTrue(extractor.hasMore());
        assertEquals(0, extractor.progressPercent());
    }

    @Test
    @DisplayName("Should throw exception when extracting before opening")
    void testExtractBeforeOpen() {
        assertThrows(IllegalStateException.class, () -> {
            extractor.extractBatch();
        });
    }

    @Test
    @DisplayName("Should close extractor and clean up resources")
    void testClose() {
        extractor.open("test");
        extractor.close();
        
        assertThrows(IllegalStateException.class, () -> {
            extractor.extractBatch();
        });
    }

    @Test
    @DisplayName("Should map supplier CUIT correctly")
    void testSupplierCuitMapping() {
        extractor.open("test");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // First record should have CUIT 30-12345678-9
        LegacyProductRecord first = batch[0];
        assertEquals("30-12345678-9", first.getSupplierCuit());
        
        // Record with missing CUIT
        LegacyProductRecord noCuit = batch[3];
        assertNull(noCuit.getSupplierCuit());
    }

    @Test
    @DisplayName("Should set prescription required flag based on active ingredient")
    void testPrescriptionRequired() {
        extractor.open("test");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // ENALAPRIL requires prescription
        LegacyProductRecord enalapril = batch[2];
        assertEquals("ENALAPRIL", enalapril.getActiveIngredient());
        assertTrue(enalapril.isPrescriptionRequired());
        
        // IBUPROFENO doesn't require prescription (in test data)
        LegacyProductRecord ibuprofeno = batch[0];
        assertFalse(ibuprofeno.isPrescriptionRequired());
    }

    @Test
    @DisplayName("Should set ANMAT category for applicable products")
    void testAnmatCategory() {
        extractor.open("test");
        
        LegacyProductRecord[] batch = extractor.extractBatch();
        
        // IBUPROFENO has BAJO_RIESGO category
        LegacyProductRecord ibuprofeno = batch[0];
        assertEquals("BAJO_RIESGO", ibuprofeno.getAnmatCategory());
    }
}
