package com.sgf.integrations.anmat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sgf.core.domain.BadRequestException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnmatDataMatrixParserTest {

    private AnmatDataMatrixParser parser;

    @BeforeEach
    void setUp() {
        parser = new AnmatDataMatrixParser();
    }

    @Test
    void shouldParseValidGs1DataMatrix() {
        AnmatDataMatrix result = parser.parse("(01)07791234567890(17)251231(10)LOTE123(21)SERIAL456");

        assertEquals("07791234567890", result.gtin());
        assertEquals(LocalDate.of(2025, 12, 31), result.expiresAt());
        assertEquals("LOTE123", result.lotNumber());
        assertEquals("SERIAL456", result.serialNumber());
    }

    @Test
    void shouldTrimWhitespaceAroundCode() {
        AnmatDataMatrix result = parser.parse("  (01)07791234567890(17)251231(10)LOTE 123 A(21)SER-456_ABC  ");

        assertEquals("LOTE 123 A", result.lotNumber());
        assertEquals("SER-456_ABC", result.serialNumber());
    }

    @Test
    void shouldRejectBlankCode() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> parser.parse(" "));
        assertEquals("DataMatrix code is required", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidFormat() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> parser.parse("INVALID_CODE_FORMAT"));
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }
}
