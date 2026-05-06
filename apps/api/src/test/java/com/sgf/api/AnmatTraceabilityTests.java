package com.sgf.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sgf.modules.core.BadRequestException;
import com.sgf.modules.integrations.anmat.service.AnmatDataMatrix;
import com.sgf.modules.integrations.anmat.service.AnmatDataMatrixParser;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AnmatTraceabilityTests {

    private final AnmatDataMatrixParser parser = new AnmatDataMatrixParser();

    @Test
    void parsesStandardAnmatDataMatrix() {
        AnmatDataMatrix value = parser.parse("(01)07791234567890(17)270101(10)LOTEXYZ(21)SERIAL123");
        assertEquals("07791234567890", value.gtin());
        assertEquals(LocalDate.of(2027, 1, 1), value.expiresAt());
        assertEquals("LOTEXYZ", value.lotNumber());
        assertEquals("SERIAL123", value.serialNumber());
    }

    @Test
    void rejectsUnsupportedFormat() {
        assertThrows(BadRequestException.class, () -> parser.parse("07791234567890|270101|LOTEXYZ|SERIAL123"));
    }
}
