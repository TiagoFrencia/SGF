package com.sgf.integrations.etl.transform;

import com.sgf.integrations.etl.LegacyProductRecord;
import com.sgf.integrations.etl.transform.DataTransformer.TransformResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataTransformer.
 * 
 * Tests cover:
 * - GTIN padding to 14 digits
 * - Active ingredient extraction from commercial name
 * - Pharmaceutical form normalization
 * - Price rounding to 2 decimal places
 * - CUIT formatting (XX-XXXXXXXX-X)
 * - Retail price estimation from cost
 * - Prescription requirement inference
 * - ANMAT traceability inference
 */
class DataTransformerTest {

    private DataTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new DataTransformer();
    }

    @Test
    @DisplayName("Should pad GTIN to 14 digits")
    void testGtinPadding() {
        LegacyProductRecord record = createBasicRecord();
        record.setGtin("7791234000010"); // 13 digits
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertEquals("00779123400010", result.record().getGtin());
        assertTrue(result.changes().stream().anyMatch(c -> c.contains("GTIN padded")));
    }

    @Test
    @DisplayName("Should extract active ingredient from commercial name")
    void testActiveIngredientExtraction() {
        LegacyProductRecord record = createBasicRecord();
        record.setCommercialName("IBUPROFENO 600mg x 20");
        record.setActiveIngredient(null); // Not set, should be extracted
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertNotNull(result.record().getActiveIngredient());
        assertEquals("IBUPROFENO", result.record().getActiveIngredient());
        assertTrue(result.changes().stream().anyMatch(c -> c.contains("IFA extracted")));
    }

    @Test
    @DisplayName("Should normalize pharmaceutical forms")
    void testFormNormalization() {
        LegacyProductRecord record = createBasicRecord();
        record.setPharmaceuticalForm("COMP");
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertEquals("COMPRIMIDOS", result.record().getPharmaceuticalForm());
        assertTrue(result.changes().stream().anyMatch(c -> c.contains("Form normalized")));
    }

    @Test
    @DisplayName("Should round prices to 2 decimal places")
    void testPriceRounding() {
        LegacyProductRecord record = createBasicRecord();
        record.setUnitCost(new BigDecimal("3500.1234"));
        record.setRetailPrice(new BigDecimal("5250.9876"));
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertEquals(new BigDecimal("3500.12"), result.record().getUnitCost());
        assertEquals(new BigDecimal("5250.99"), result.record().getRetailPrice());
        assertTrue(result.changes().size() >= 2); // Both prices rounded
    }

    @Test
    @DisplayName("Should format CUIT as XX-XXXXXXXX-X")
    void testCuitFormatting() {
        LegacyProductRecord record = createBasicRecord();
        record.setSupplierCuit("30123456789"); // No dashes
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertEquals("30-12345678-9", result.record().getSupplierCuit());
        assertTrue(result.changes().stream().anyMatch(c -> c.contains("CUIT formatted")));
    }

    @Test
    @DisplayName("Should estimate retail price from cost when missing")
    void testPriceEstimation() {
        LegacyProductRecord record = createBasicRecord();
        record.setUnitCost(new BigDecimal("2000.00"));
        record.setRetailPrice(null);
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertNotNull(result.record().getRetailPrice());
        assertEquals(new BigDecimal("3000.00"), result.record().getRetailPrice()); // 2000 * 1.5
        assertTrue(result.changes().stream().anyMatch(c -> c.contains("Price estimated")));
    }

    @Test
    @DisplayName("Should infer prescription requirement from active ingredient")
    void testPrescriptionInference() {
        LegacyProductRecord record = createBasicRecord();
        record.setActiveIngredient("ENALAPRIL");
        record.setPrescriptionRequired(false);
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertTrue(result.record().isPrescriptionRequired());
        assertTrue(result.changes().stream().anyMatch(c -> c.contains("Prescription required")));
    }

    @Test
    @DisplayName("Should infer ANMAT traceability for high-risk ingredients")
    void testAnmatTraceabilityInference() {
        LegacyProductRecord record = createBasicRecord();
        record.setActiveIngredient("INMUNOGLOBULINA");
        record.setRequiresTraceability(false);
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertTrue(result.record().isRequiresTraceability());
        assertTrue(result.changes().stream().anyMatch(c -> c.contains("ANMAT traceability")));
    }

    @Test
    @DisplayName("Should mark expired products with EXPIRED status")
    void testExpiredStatus() {
        LegacyProductRecord record = createBasicRecord();
        record.setExpiryDate(LocalDate.now().minusDays(30));
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertEquals("EXPIRED", result.status());
        assertFalse(result.isClean());
    }

    @Test
    @DisplayName("Should mark products without GTIN as MISSING_GTIN")
    void testMissingGtinStatus() {
        LegacyProductRecord record = createBasicRecord();
        record.setGtin(null);
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertEquals("MISSING_GTIN", result.status());
        assertFalse(result.isClean());
    }

    @Test
    @DisplayName("Should return CLEAN status for valid records")
    void testCleanStatus() {
        LegacyProductRecord record = createBasicRecord();
        record.setGtin("7791234000010");
        record.setExpiryDate(LocalDate.now().plusYears(2));
        
        List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
        
        TransformResult result = results.get(0);
        assertEquals("CLEAN", result.status());
        assertTrue(result.isClean());
    }

    @Test
    @DisplayName("Should transform batch of records")
    void testBatchTransform() {
        LegacyProductRecord[] batch = new LegacyProductRecord[3];
        for (int i = 0; i < 3; i++) {
            batch[i] = createBasicRecord();
            batch[i].setGtin("77912340000" + (i + 1));
        }
        
        List<TransformResult> results = transformer.transform(batch);
        
        assertEquals(3, results.size());
        for (int i = 0; i < 3; i++) {
            assertEquals("007791234000" + (i + 1), results.get(i).record().getGtin());
        }
    }

    @Test
    @DisplayName("Should handle multiple active ingredients requiring prescription")
    void testMultiplePrescriptionIngredients() {
        String[] prescriptionDrugs = {"LOSARTAN", "ATORVASTATINA", "SIMVASTATINA", 
                                       "LEVOTIROXINA", "CLARITROMICINA", "AMOXICILINA",
                                       "OMEPRAZOL", "METFORMINA", "SILDENAFILO",
                                       "RANITIDINA", "CLOPIDOGREL", "WARFARINA"};
        
        for (String drug : prescriptionDrugs) {
            LegacyProductRecord record = createBasicRecord();
            record.setActiveIngredient(drug);
            record.setPrescriptionRequired(false);
            
            List<TransformResult> results = transformer.transform(new LegacyProductRecord[]{record});
            
            assertTrue(results.get(0).record().isPrescriptionRequired(), 
                      "Drug " + drug + " should require prescription");
        }
    }

    // Helper method
    private LegacyProductRecord createBasicRecord() {
        LegacyProductRecord record = new LegacyProductRecord();
        record.setLegacyId("TEST-001");
        record.setCommercialName("Producto Test");
        record.setActiveIngredient("Principio Activo");
        record.setConcentration("500mg");
        record.setPharmaceuticalForm("COMPRIMIDOS");
        record.setBrand("Laboratorio Test");
        record.setCurrentStock(100);
        record.setUnitCost(new BigDecimal("1000.00"));
        record.setRetailPrice(new BigDecimal("1500.00"));
        record.setExpiryDate(LocalDate.now().plusYears(2));
        record.setSupplierCuit("30-12345678-9");
        return record;
    }
}
