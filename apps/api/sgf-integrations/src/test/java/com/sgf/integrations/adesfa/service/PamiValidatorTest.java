package com.sgf.integrations.adesfa.service;

import com.sgf.integrations.adesfa.web.AdesfaValidationRequest;
import com.sgf.integrations.adesfa.web.AdesfaValidationRequest.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PamiValidator
 * Valida las reglas de cobertura del PAMI (70/30)
 */
class PamiValidatorTest {

    private PamiValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PamiValidator();
    }

    @Test
    @DisplayName("Debe retornar código de validador PAMI")
    void shouldReturnPamiValidatorCode() {
        // When
        String code = validator.getValidatorCode();

        // Then
        assertEquals("PAMI", code);
    }

    @Test
    @DisplayName("Debe aplicar cobertura 70% para medicamentos crónicos")
    void shouldApply70PercentCoverage_ForChronicMedications() {
        // Given
        List<Item> items = List.of(
            new Item("MED-001", "Ibuprofeno", 2, BigDecimal.valueOf(1000.00))
        );
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.approved());
        assertEquals("APPROVED_PAMI", result.status());
        assertTrue(result.message().contains("Cobertura 70%"));
        
        BigDecimal total = BigDecimal.valueOf(2000.00); // 2 * 1000
        BigDecimal expectedCoverage = total.multiply(new BigDecimal("0.70")); // 1400
        BigDecimal expectedPatientPay = total.multiply(new BigDecimal("0.30")); // 600
        
        assertEquals(expectedCoverage, result.coverageAmount());
        assertEquals(expectedPatientPay, result.patientPayAmount());
        assertNotNull(result.authorizationCode());
    }

    @Test
    @DisplayName("Debe calcular correctamente cobertura para múltiples items")
    void shouldCalculateCoverage_ForMultipleItems() {
        // Given
        List<Item> items = List.of(
            new Item("MED-001", "Metformina", 3, BigDecimal.valueOf(500.00)),
            new Item("MED-002", "Losartán", 2, BigDecimal.valueOf(800.00)),
            new Item("MED-003", "Atorvastatina", 1, BigDecimal.valueOf(1200.00))
        );
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.approved());
        
        // Total: (3*500) + (2*800) + (1*1200) = 1500 + 1600 + 1200 = 4300
        BigDecimal expectedTotal = BigDecimal.valueOf(4300.00);
        BigDecimal expectedCoverage = expectedTotal.multiply(new BigDecimal("0.70")); // 3010
        BigDecimal expectedPatientPay = expectedTotal.multiply(new BigDecimal("0.30")); // 1290
        
        assertEquals(expectedCoverage, result.coverageAmount());
        assertEquals(expectedPatientPay, result.patientPayAmount());
    }

    @Test
    @DisplayName("Debe manejar cantidades decimales correctamente")
    void shouldHandleDecimalQuantities() {
        // Given
        List<Item> items = List.of(
            new Item("MED-001", "Insulina", 1, BigDecimal.valueOf("1500.50"))
        );
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.approved());
        
        BigDecimal total = BigDecimal.valueOf(1500.50);
        BigDecimal expectedCoverage = total.multiply(new BigDecimal("0.70")); // 1050.35
        BigDecimal expectedPatientPay = total.multiply(new BigDecimal("0.30")); // 450.15
        
        assertEquals(expectedCoverage, result.coverageAmount());
        assertEquals(expectedPatientPay, result.patientPayAmount());
    }

    @Test
    @DisplayName("Debe generar código de autorización único por validación")
    void shouldGenerateUniqueAuthorizationCode_PerValidation() {
        // Given
        List<Item> items = List.of(
            new Item("MED-001", "Aspirina", 1, BigDecimal.valueOf(100.00))
        );
        AdesfaValidationRequest request1 = createValidRequest(items);
        AdesfaValidationRequest request2 = createValidRequest(items);

        // When
        var result1 = validator.validate(request1);
        var result2 = validator.validate(request2);

        // Then
        assertNotNull(result1.authorizationCode());
        assertNotNull(result2.authorizationCode());
        assertNotEquals(result1.authorizationCode(), result2.authorizationCode());
    }

    @Test
    @DisplayName("Debe manejar lista vacía de items sin error")
    void shouldHandleEmptyItemList() {
        // Given
        List<Item> items = List.of();
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.approved());
        assertEquals(BigDecimal.ZERO, result.coverageAmount());
        assertEquals(BigDecimal.ZERO, result.patientPayAmount());
    }

    @Test
    @DisplayName("Debe manejar item con precio cero")
    void shouldHandleItemWithZeroPrice() {
        // Given
        List<Item> items = List.of(
            new Item("MED-001", "Muestra gratuita", 5, BigDecimal.ZERO)
        );
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.approved());
        assertEquals(BigDecimal.ZERO, result.coverageAmount());
        assertEquals(BigDecimal.ZERO, result.patientPayAmount());
    }

    @Test
    @DisplayName("Debe manejar un solo item con cantidad grande")
    void shouldHandleSingleItemWithLargeQuantity() {
        // Given
        List<Item> items = List.of(
            new Item("MED-001", "Paracetamol", 100, BigDecimal.valueOf(10.00))
        );
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.approved());
        
        BigDecimal total = BigDecimal.valueOf(1000.00); // 100 * 10
        BigDecimal expectedCoverage = total.multiply(new BigDecimal("0.70")); // 700
        BigDecimal expectedPatientPay = total.multiply(new BigDecimal("0.30")); // 300
        
        assertEquals(expectedCoverage, result.coverageAmount());
        assertEquals(expectedPatientPay, result.patientPayAmount());
    }

    @Test
    @DisplayName("Debe mantener precisión decimal en cálculos")
    void shouldMaintainDecimalPrecision_InCalculations() {
        // Given
        List<Item> items = List.of(
            new Item("MED-001", "Medicamento preciso", 3, BigDecimal.valueOf("333.33"))
        );
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.approved());
        
        BigDecimal total = BigDecimal.valueOf("999.99"); // 3 * 333.33
        BigDecimal expectedCoverage = total.multiply(new BigDecimal("0.70")); // 699.993
        BigDecimal expectedPatientPay = total.multiply(new BigDecimal("0.30")); // 299.997
        
        assertEquals(expectedCoverage, result.coverageAmount());
        assertEquals(expectedPatientPay, result.patientPayAmount());
    }

    @Test
    @DisplayName("Debe incluir mensaje de éxito descriptivo")
    void shouldIncludeDescriptiveSuccessMessage() {
        // Given
        List<Item> items = List.of(
            new Item("MED-001", "Test", 1, BigDecimal.valueOf(100.00))
        );
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.message().contains("Validación PAMI Exitosa"));
        assertTrue(result.message().contains("Cobertura 70%"));
    }

    @Test
    @DisplayName("Debe retornar siempre aprobado para PAMI")
    void shouldAlwaysReturnApproved_ForPami() {
        // Given - cualquier combinación de items
        List<Item> items = List.of(
            new Item("MED-001", "Item 1", 1, BigDecimal.valueOf(50.00)),
            new Item("MED-002", "Item 2", 2, BigDecimal.valueOf(75.00)),
            new Item("MED-003", "Item 3", 3, BigDecimal.valueOf(25.00))
        );
        AdesfaValidationRequest request = createValidRequest(items);

        // When
        var result = validator.validate(request);

        // Then
        assertTrue(result.approved());
    }

    // Helper method
    private AdesfaValidationRequest createValidRequest(List<Item> items) {
        return new AdesfaValidationRequest(
            "PAMI",
            "20123456789", // CUIT
            "Juan Pérez",
            items
        );
    }
}
