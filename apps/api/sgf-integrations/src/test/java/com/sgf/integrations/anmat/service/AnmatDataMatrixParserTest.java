package com.sgf.integrations.anmat.service;

import com.sgf.core.domain.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para AnmatDataMatrixParser
 * Valida el parseo de códigos DataMatrix GS1 para trazabilidad ANMAT
 */
class AnmatDataMatrixParserTest {

    private AnmatDataMatrixParser parser;

    @BeforeEach
    void setUp() {
        parser = new AnmatDataMatrixParser();
    }

    @Test
    @DisplayName("Debe parsear correctamente un DataMatrix GS1 válido con formato completo")
    void shouldParseValidDataMatrix_WhenFormatIsCorrect() {
        // Given
        String validDataMatrix = "(01)07791234567890(17)251231(10)LOTE123(21)SERIAL456";

        // When
        AnmatDataMatrix result = parser.parse(validDataMatrix);

        // Then
        assertNotNull(result);
        assertEquals("07791234567890", result.gtin());
        assertEquals(LocalDate.of(2025, 12, 31), result.expirationDate());
        assertEquals("LOTE123", result.batch());
        assertEquals("SERIAL456", result.serial());
    }

    @Test
    @DisplayName("Debe parsear correctamente un DataMatrix con fecha de vencimiento válida")
    void shouldParseDataMatrix_WhenExpirationDateIsValid() {
        // Given
        String dataMatrix = "(01)07798765432109(17)240615(10)ABC123(21)XYZ789";

        // When
        AnmatDataMatrix result = parser.parse(dataMatrix);

        // Then
        assertEquals("07798765432109", result.gtin());
        assertEquals(LocalDate.of(2024, 6, 15), result.expirationDate());
        assertEquals("ABC123", result.batch());
        assertEquals("XYZ789", result.serial());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el código es null")
    void shouldThrowException_WhenCodeIsNull() {
        // Given
        String nullCode = null;

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(nullCode)
        );
        assertEquals("DataMatrix code is required", exception.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el código está vacío")
    void shouldThrowException_WhenCodeIsEmpty() {
        // Given
        String emptyCode = "";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(emptyCode)
        );
        assertEquals("DataMatrix code is required", exception.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el código solo tiene espacios en blanco")
    void shouldThrowException_WhenCodeIsBlank() {
        // Given
        String blankCode = "   ";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(blankCode)
        );
        assertEquals("DataMatrix code is required", exception.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el formato no coincide con GS1 esperado")
    void shouldThrowException_WhenFormatDoesNotMatch() {
        // Given
        String invalidFormat = "INVALID_CODE_FORMAT";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(invalidFormat)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
        assertTrue(exception.getMessage().contains("(01)GTIN(17)VENC(10)LOTE(21)SERIAL"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando falta el GTIN (01)")
    void shouldThrowException_WhenMissingGtin() {
        // Given
        String missingGtin = "(17)251231(10)LOTE123(21)SERIAL456";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(missingGtin)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando falta la fecha de vencimiento (17)")
    void shouldThrowException_WhenMissingExpirationDate() {
        // Given
        String missingVenc = "(01)07791234567890(10)LOTE123(21)SERIAL456";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(missingVenc)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando falta el lote (10)")
    void shouldThrowException_WhenMissingBatch() {
        // Given
        String missingLote = "(01)07791234567890(17)251231(21)SERIAL456";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(missingLote)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando falta el serial (21)")
    void shouldThrowException_WhenMissingSerial() {
        // Given
        String missingSerial = "(01)07791234567890(17)251231(10)LOTE123";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(missingSerial)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el GTIN no tiene 14 dígitos")
    void shouldThrowException_WhenGtinNot14Digits() {
        // Given
        String invalidGtin = "(01)1234567890(17)251231(10)LOTE123(21)SERIAL456";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(invalidGtin)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la fecha no tiene formato YYMMDD válido")
    void shouldThrowException_WhenInvalidDateFormat() {
        // Given
        String invalidDate = "(01)07791234567890(17)2025123(10)LOTE123(21)SERIAL456";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(invalidDate)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }

    @Test
    @DisplayName("Debe trimpear espacios en blanco al inicio y fin del código")
    void shouldTrimWhitespace_FromCode() {
        // Given
        String codeWithSpaces = "  (01)07791234567890(17)251231(10)LOTE123(21)SERIAL456  ";

        // When
        AnmatDataMatrix result = parser.parse(codeWithSpaces);

        // Then
        assertNotNull(result);
        assertEquals("07791234567890", result.gtin());
        assertEquals(LocalDate.of(2025, 12, 31), result.expirationDate());
        assertEquals("LOTE123", result.batch());
        assertEquals("SERIAL456", result.serial());
    }

    @Test
    @DisplayName("Debe manejar correctamente lotes con espacios internos")
    void shouldHandleBatchWithInternalSpaces() {
        // Given
        String dataMatrix = "(01)07791234567890(17)251231(10)LOTE 123 A(21)SERIAL456";

        // When
        AnmatDataMatrix result = parser.parse(dataMatrix);

        // Then
        assertEquals("LOTE 123 A", result.batch());
    }

    @Test
    @DisplayName("Debe manejar correctamente seriales con caracteres especiales")
    void shouldHandleSerialWithSpecialCharacters() {
        // Given
        String dataMatrix = "(01)07791234567890(17)251231(10)LOTE123(21)SER-456_ABC";

        // When
        AnmatDataMatrix result = parser.parse(dataMatrix);

        // Then
        assertEquals("SER-456_ABC", result.serial());
    }

    @Test
    @DisplayName("Debe parsear correctamente un DataMatrix con año 2030+")
    void shouldParseDataMatrix_WhenYearIs2030OrLater() {
        // Given
        // Formato YY: 30 = 2030
        String dataMatrix = "(01)07791234567890(17)301231(10)LOTE123(21)SERIAL456";

        // When
        AnmatDataMatrix result = parser.parse(dataMatrix);

        // Then
        assertEquals(LocalDate.of(2030, 12, 31), result.expirationDate());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando hay paréntesis adicionales en el valor del lote")
    void shouldThrowException_WhenExtraParenthesesInBatch() {
        // Given
        String invalidBatch = "(01)07791234567890(17)251231(10)LOT(E)123(21)SERIAL456";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(invalidBatch)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }

    @Test
    @DisplayName("Debe validar que el serial no contenga paréntesis")
    void shouldValidateSerialWithoutParentheses() {
        // Given
        String invalidSerial = "(01)07791234567890(17)251231(10)LOTE123(21)SER(IAL)456";

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> parser.parse(invalidSerial)
        );
        assertTrue(exception.getMessage().contains("Unsupported DataMatrix format"));
    }
}
