package com.sgf.integrations.etl.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sgf.integrations.etl.LegacyProductRecord;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FarmaWinExtractorTest {

    private FarmaWinExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new FarmaWinExtractor();
    }

    @Test
    void shouldReturnFarmaWinAsSourceSystemName() {
        assertEquals("FarmaWin", extractor.sourceSystemName());
    }

    @Test
    void shouldOpenAndLoadSampleRecords() {
        extractor.open("test-connection");
        assertTrue(extractor.totalRecords() > 0);
    }

    @Test
    void shouldExtractBatchUsingCurrentApi() {
        extractor.open("test-connection");

        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasMore()) {
            records.addAll(Arrays.asList(extractor.extractBatch()));
        }

        assertFalse(records.isEmpty());
        LegacyProductRecord firstValid = records.stream()
                .filter(r -> r.getGtin() != null && !r.getGtin().isBlank())
                .findFirst()
                .orElseThrow();

        assertNotNull(firstValid.getLegacyId());
        assertNotNull(firstValid.getGtin());
        assertNotNull(firstValid.getCommercialName());
        assertNotNull(firstValid.getActiveIngredient());
        assertNotNull(firstValid.getPharmaceuticalForm());
        assertNotNull(firstValid.getBrand());
        assertNotNull(firstValid.getUnitCost());
        assertNotNull(firstValid.getLotNumber());
        assertNotNull(firstValid.getExpiryDate());
    }

    @Test
    void shouldExposeExpiredSampleRecord() {
        extractor.open("test-connection");

        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasMore()) {
            records.addAll(Arrays.asList(extractor.extractBatch()));
        }

        boolean hasExpired = records.stream()
                .anyMatch(record -> record.getExpiryDate() != null && record.getExpiryDate().isBefore(LocalDate.now()));

        assertTrue(hasExpired);
    }

    @Test
    void shouldResetCursor() {
        extractor.open("test-connection");
        extractor.extractBatch();
        assertTrue(extractor.progressPercent() > 0);

        extractor.reset();

        assertEquals(0, extractor.progressPercent());
        assertTrue(extractor.hasMore());
    }
}
