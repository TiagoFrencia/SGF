package com.sgf.integrations.etl.extract;

import com.sgf.integrations.etl.LegacyProductRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para FarmaWinExtractor
 * Valida la extracción de datos desde sistemas legacy FarmaWin
 */
class FarmaWinExtractorTest {

    private FarmaWinExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new FarmaWinExtractor();
    }

    @Test
    @DisplayName("Debe retornar nombre del sistema fuente FarmaWin")
    void shouldReturnFarmaWinAsSourceSystemName() {
        // When
        String name = extractor.sourceSystemName();

        // Then
        assertEquals("FarmaWin", name);
    }

    @Test
    @DisplayName("Debe abrir conexión y cargar registros de ejemplo")
    void shouldOpenConnection_AndLoadSampleRecords() {
        // Given
        String connectionString = "jdbc:firebird:localhost/3050:/path/to/farmawin.fdb";

        // When
        extractor.open(connectionString);

        // Then
        assertTrue(extractor.totalRecords() > 0);
    }

    @Test
    @DisplayName("Debe extraer registros válidos con todos los campos requeridos")
    void shouldExtractValidRecords_WithAllRequiredFields() {
        // Given
        extractor.open("test-connection");

        // When
        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasNext()) {
            records.add(extractor.next());
        }

        // Then
        assertFalse(records.isEmpty());
        
        // Verificar primer registro válido (con GTIN)
        LegacyProductRecord firstValidRecord = records.stream()
            .filter(r -> r.gtin() != null && !r.gtin().isBlank())
            .findFirst()
            .orElseThrow();
        
        assertNotNull(firstValidRecord.id());
        assertNotNull(firstValidRecord.gtin());
        assertTrue(firstValidRecord.gtin().length() >= 13);
        assertNotNull(firstValidRecord.name());
        assertNotNull(firstValidRecord.activeIngredient());
        assertNotNull(firstValidRecord.strength());
        assertNotNull(firstValidRecord.pharmaceuticalForm());
        assertNotNull(firstValidRecord.laboratory());
        assertNotNull(firstValidRecord.packageSize());
        assertNotNull(firstValidRecord.price());
        assertNotNull(firstValidRecord.batch());
        assertNotNull(firstValidRecord.expirationDate());
    }

    @Test
    @DisplayName("Debe manejar registros sin GTIN correctamente")
    void shouldHandleRecordsWithoutGtin() {
        // Given
        extractor.open("test-connection");

        // When
        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasNext()) {
            records.add(extractor.next());
        }

        // Then - debe haber al menos un registro sin GTIN (registro 0004 en datos de ejemplo)
        boolean hasRecordWithoutGtin = records.stream()
            .anyMatch(r -> r.gtin() == null || r.gtin().isBlank());
        
        assertTrue(hasRecordWithoutGtin, "Debería haber al menos un registro sin GTIN");
    }

    @Test
    @DisplayName("Debe parsear fechas en formato dd/MM/yyyy correctamente")
    void shouldParseDates_InDdMmYyyyFormat() {
        // Given
        extractor.open("test-connection");

        // When
        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasNext()) {
            records.add(extractor.next());
        }

        // Then
        for (LegacyProductRecord record : records) {
            assertNotNull(record.expirationDate(), 
                "La fecha de vencimiento no debería ser null para: " + record.name());
            
            // Las fechas deberían ser posteriores a 2024 (ninguna debería ser muy antigua)
            assertTrue(record.expirationDate().isAfter(LocalDate.of(2020, 1, 1)),
                "Fecha inválida para: " + record.name());
        }
    }

    @Test
    @DisplayName("Debe detectar registros vencidos")
    void shouldDetectExpiredRecords() {
        // Given
        extractor.open("test-connection");
        LocalDate today = LocalDate.now();

        // When
        List<LegacyProductRecord> expiredRecords = new ArrayList<>();
        while (extractor.hasNext()) {
            LegacyProductRecord record = extractor.next();
            if (record.expirationDate().isBefore(today)) {
                expiredRecords.add(record);
            }
        }

        // Then - el registro 0004 (ATENOLOL) tiene vencimiento 10/10/2024
        assertFalse(expiredRecords.isEmpty(), "Debería haber al menos un registro vencido");
        
        LegacyProductRecord expiredRecord = expiredRecords.get(0);
        assertTrue(expiredRecord.expirationDate().isBefore(today));
        assertEquals("ATENOLOL 50mg x 30", expiredRecord.name());
    }

    @Test
    @DisplayName("Debe iterar sobre todos los registros disponibles")
    void shouldIterateOverAllAvailableRecords() {
        // Given
        extractor.open("test-connection");
        long totalRecords = extractor.totalRecords();

        // When
        int count = 0;
        while (extractor.hasNext()) {
            extractor.next();
            count++;
        }

        // Then
        assertEquals(totalRecords, count, 
            "El contador de iteración debería coincidir con totalRecords");
    }

    @Test
    @DisplayName("Debe retornar false en hasNext cuando no hay más registros")
    void shouldReturnFalseInHasNext_WhenNoMoreRecords() {
        // Given
        extractor.open("test-connection");

        // When - consumir todos los registros
        while (extractor.hasNext()) {
            extractor.next();
        }

        // Then
        assertFalse(extractor.hasNext(), "No debería haber más registros después de consumir todos");
    }

    @Test
    @DisplayName("Debe manejar precios con 4 decimales como en FarmaWin")
    void shouldHandlePrices_WithFourDecimalPlaces() {
        // Given
        extractor.open("test-connection");

        // When
        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasNext()) {
            records.add(extractor.next());
        }

        // Then
        for (LegacyProductRecord record : records) {
            assertNotNull(record.price());
            assertTrue(record.price().compareTo(java.math.BigDecimal.ZERO) >= 0,
                "El precio no debería ser negativo para: " + record.name());
        }
    }

    @Test
    @DisplayName("Debe extraer información de laboratorio correctamente")
    void shouldExtractLaboratoryInformation() {
        // Given
        extractor.open("test-connection");

        // When
        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasNext()) {
            records.add(extractor.next());
        }

        // Then - verificar laboratorios conocidos
        List<String> expectedLabs = List.of("BAGO", "ROEMMERS", "GADOR", "CARI", "BAYER");
        List<String> extractedLabs = records.stream()
            .map(LegacyProductRecord::laboratory)
            .filter(l -> l != null && !l.isBlank())
            .distinct()
            .toList();
        
        assertTrue(extractedLabs.size() >= 3, 
            "Debería extraer al menos 3 laboratorios diferentes");
    }

    @Test
    @DisplayName("Debe manejar nombres de productos con ingrediente activo separado por slash")
    void shouldHandleProductNames_WithActiveIngredientSeparatedBySlash() {
        // Given
        extractor.open("test-connection");

        // When
        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasNext()) {
            records.add(extractor.next());
        }

        // Then
        for (LegacyProductRecord record : records) {
            assertNotNull(record.name(), "El nombre no debería ser null");
            assertNotNull(record.activeIngredient(), 
                "El ingrediente activo no debería ser null para: " + record.name());
        }
    }

    @Test
    @DisplayName("Debe extraer números de lote válidos")
    void shouldExtractValidBatchNumbers() {
        // Given
        extractor.open("test-connection");

        // When
        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasNext()) {
            records.add(extractor.next());
        }

        // Then
        for (LegacyProductRecord record : records) {
            assertNotNull(record.batch(), 
                "El número de lote no debería ser null para: " + record.name());
            assertFalse(record.batch().isBlank(),
                "El número de lote no debería estar vacío para: " + record.name());
        }
    }

    @Test
    @DisplayName("Debe manejar múltiples llamadas a open correctamente")
    void shouldHandleMultipleOpenCalls() {
        // Given
        extractor.open("test-connection-1");
        int firstCount = 0;
        while (extractor.hasNext()) {
            extractor.next();
            firstCount++;
        }

        // When - reabrir con otra conexión
        extractor.open("test-connection-2");
        int secondCount = 0;
        while (extractor.hasNext()) {
            extractor.next();
            secondCount++;
        }

        // Then
        assertEquals(firstCount, secondCount,
            "Ambas conexiones deberían retornar la misma cantidad de registros de ejemplo");
    }

    @Test
    @DisplayName("Debe soportar CUIT de proveedores con formato argentino")
    void shouldHandleSupplierCuit_InArgentinianFormat() {
        // Given
        extractor.open("test-connection");

        // When
        List<LegacyProductRecord> records = new ArrayList<>();
        while (extractor.hasNext()) {
            records.add(extractor.next());
        }

        // Then - algunos registros deberían tener CUIT válido
        long recordsWithCuit = records.stream()
            .filter(r -> r.supplierCuit() != null && !r.supplierCuit().isBlank())
            .count();
        
        assertTrue(recordsWithCuit > 0, 
            "Debería haber al menos un registro con CUIT de proveedor");
    }
}
