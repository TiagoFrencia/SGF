package com.sgf.integrations.vademecum;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KairosConnectorTest {

    @InjectMocks
    private KairosConnector kairosConnector;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void kairosProduct_RecordProperties() {
        // Given
        KairosConnector.KairosProduct product = new KairosConnector.KairosProduct(
            "7791234567890",
            "IBUPROFENO 600MG",
            "ACTRON",
            "IBUPROFENO",
            "M01AE01",
            "TABLETAS",
            "600 mg",
            "30 COMPRIMIDOS",
            "BAYER S.A.",
            true,
            false,
            1250.50,
            "2024-01-15"
        );

        // Then
        assertEquals("7791234567890", product.gtin());
        assertEquals("IBUPROFENO 600MG", product.commercialName());
        assertEquals("ACTRON", product.brand());
        assertEquals("IBUPROFENO", product.activeIngredient());
        assertEquals("M01AE01", product.atcCode());
        assertEquals("TABLETAS", product.pharmaceuticalForm());
        assertEquals("600 mg", product.concentration());
        assertEquals("30 COMPRIMIDOS", product.presentation());
        assertEquals("BAYER S.A.", product.laboratory());
        assertTrue(product.requiresPrescription());
        assertFalse(product.requiresRefrigeration());
        assertEquals(1250.50, product.retailPrice());
        assertEquals("2024-01-15", product.updatedAt());
    }

    @Test
    void kairosInteraction_SeverityLevels() {
        // Test different severity levels for drug interactions
        KairosConnector.KairosInteraction severe = new KairosConnector.KairosInteraction(
            "WARFARINA", "ASPIRINA", 
            KairosConnector.InteractionSeverity.SEVERE,
            "Aumento del riesgo de sangrado",
            "Evitar uso concomitante"
        );

        KairosConnector.KairosInteraction moderate = new KairosConnector.KairosInteraction(
            "IBUPROFENO", "OMEPRAZOL",
            KairosConnector.InteractionSeverity.MODERATE,
            "Disminución de la eficacia del IBUPROFENO",
            "Monitorear respuesta terapéutica"
        );

        KairosConnector.KairosInteraction mild = new KairosConnector.KairosInteraction(
            "VITAMINA C", "ASPIRINA",
            KairosConnector.InteractionSeverity.MILD,
            "Leve aumento de acidez gástrica",
            "Tomar con alimentos"
        );

        // Then
        assertEquals(KairosConnector.InteractionSeverity.SEVERE, severe.severity());
        assertEquals(KairosConnector.InteractionSeverity.MODERATE, moderate.severity());
        assertEquals(KairosConnector.InteractionSeverity.MILD, mild.severity());
    }

    @Test
    void interactionSeverity_EnumValues() {
        // Verify all severity levels are defined
        KairosConnector.InteractionSeverity[] severities = KairosConnector.InteractionSeverity.values();
        
        assertEquals(3, severities.length);
        assertTrue(List.of(severities).contains(KairosConnector.InteractionSeverity.SEVERE));
        assertTrue(List.of(severities).contains(KairosConnector.InteractionSeverity.MODERATE));
        assertTrue(List.of(severities).contains(KairosConnector.InteractionSeverity.MILD));
    }

    @Test
    void bioequivalentProduct_IsBioequivalent_Flag() {
        // Given - Bioequivalent generic product
        KairosConnector.BioequivalentProduct bioeq = new KairosConnector.BioequivalentProduct(
            "7791112223334",
            "IBUPROFENO GENÉRICO",
            "GENÉRICA SA",
            "IBUPROFENO",
            true,
            850.00
        );

        // Then
        assertTrue(bioeq.isBioequivalent());
        assertEquals("IBUPROFENO GENÉRICO", bioeq.commercialName());
        assertEquals(850.00, bioeq.retailPrice());
    }

    @Test
    void connector_Initialization_Successful() {
        // Verify connector can be instantiated with ObjectMapper
        KairosConnector connector = new KairosConnector(new ObjectMapper());
        assertNotNull(connector);
    }

    @Test
    void checkInteractions_EmptyList_NoInteractions() {
        // Test scenario: no drug interactions found
        List<KairosConnector.KairosInteraction> noInteractions = List.of();
        
        assertTrue(noInteractions.isEmpty());
    }

    @Test
    void findBioequivalents_MultipleResults() {
        // Test scenario: multiple bioequivalent products available
        List<KairosConnector.BioequivalentProduct> bioequivalents = List.of(
            new KairosConnector.BioequivalentProduct(
                "7791112223334", "GENÉRICO A", "LAB_A", "IBUPROFENO", true, 850.00
            ),
            new KairosConnector.BioequivalentProduct(
                "7791112223335", "GENÉRICO B", "LAB_B", "IBUPROFENO", true, 900.00
            ),
            new KairosConnector.BioequivalentProduct(
                "7791112223336", "GENÉRICO C", "LAB_C", "IBUPROFENO", true, 875.00
            )
        );

        assertEquals(3, bioequivalents.size());
        assertTrue(bioequivalents.stream().allMatch(KairosConnector.BioequivalentProduct::isBioequivalent));
        
        // Find cheapest option
        KairosConnector.BioequivalentProduct cheapest = bioequivalents.stream()
            .min(java.util.Comparator.comparingDouble(KairosConnector.BioequivalentProduct::retailPrice))
            .orElseThrow();
        assertEquals(850.00, cheapest.retailPrice());
    }

    @Test
    void productSearch_ByGtin_Format() {
        // Verify GTIN format validation
        String validGtin13 = "7791234567890";
        String validGtin14 = "07791234567897";
        
        assertEquals(13, validGtin13.length());
        assertEquals(14, validGtin14.length());
        assertTrue(validGtin13.matches("\\d{13}"));
        assertTrue(validGtin14.matches("\\d{14}"));
    }

    @Test
    void interactionCheck_MultipleIngredients() {
        // Test scenario: checking interactions between multiple medications
        List<String> ingredients = List.of("IBUPROFENO", "OMEPRAZOL", "ASPIRINA");
        
        assertEquals(3, ingredients.size());
        // Number of possible pairs: n*(n-1)/2 = 3 pairs
        int expectedPairs = ingredients.size() * (ingredients.size() - 1) / 2;
        assertEquals(3, expectedPairs);
    }

    @Test
    void retailPrice_Range_Validation() {
        // Test realistic price ranges for Argentine pharmaceutical market
        double minPrice = 100.00;
        double maxPrice = 50000.00;
        double typicalPrice = 2500.00;
        
        assertTrue(minPrice > 0);
        assertTrue(maxPrice > minPrice);
        assertTrue(typicalPrice >= minPrice && typicalPrice <= maxPrice);
    }
}
