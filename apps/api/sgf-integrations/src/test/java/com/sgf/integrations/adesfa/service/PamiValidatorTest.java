package com.sgf.integrations.adesfa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sgf.integrations.adesfa.domain.AdesfaValidationResult;
import com.sgf.integrations.adesfa.web.AdesfaValidationRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PamiValidatorTest {

    private PamiValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PamiValidator();
    }

    @Test
    void shouldReturnPamiValidatorCode() {
        assertEquals("PAMI", validator.getValidatorCode());
    }

    @Test
    void shouldApplyStandardSeventyThirtySplit() {
        AdesfaValidationRequest request = new AdesfaValidationRequest(
                "PAMI",
                "01",
                "1234567890",
                "PRES-123",
                List.of(
                        new AdesfaValidationRequest.ValidationItemRequest(UUID.randomUUID(), 2, new BigDecimal("1000.00")),
                        new AdesfaValidationRequest.ValidationItemRequest(UUID.randomUUID(), 1, new BigDecimal("500.00"))
                )
        );

        AdesfaValidationResult result = validator.validate(request);

        assertTrue(result.success());
        assertEquals("APPROVED_PAMI", result.status());
        assertEquals(new BigDecimal("1750.0000"), result.coverageAmount());
        assertEquals(new BigDecimal("750.0000"), result.patientAmount());
        assertNotNull(result.reference());
        assertFalse(result.reference().isBlank());
    }

    @Test
    void shouldHandleEmptyItemsWithoutError() {
        AdesfaValidationRequest request = new AdesfaValidationRequest(
                "PAMI",
                "01",
                "1234567890",
                "PRES-EMPTY",
                List.of()
        );

        AdesfaValidationResult result = validator.validate(request);

        assertTrue(result.success());
        assertEquals(0, result.coverageAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.patientAmount().compareTo(BigDecimal.ZERO));
    }
}
