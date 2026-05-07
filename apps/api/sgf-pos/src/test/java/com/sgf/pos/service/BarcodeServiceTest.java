package com.sgf.pos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.service.InventoryService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarcodeServiceTest {

    @InjectMocks
    private BarcodeService barcodeService;

    @Test
    void parse_Ean13_ValidBarcode() {
        // Given - Valid EAN-13 code (7791234567890 with correct check digit)
        String ean13 = "7791234567897"; // Corrected check digit
        
        // When
        BarcodeService.ParsedBarcode result = barcodeService.parse(ean13);

        // Then
        assertNotNull(result);
        assertEquals("7791234567897", result.gtin());
        assertTrue(result.isValid());
    }

    @Test
    void parse_Ean13_InvalidChecksum_ThrowsException() {
        // Given - Invalid EAN-13 (wrong check digit)
        String invalidEan13 = "7791234567890";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            barcodeService.parse(invalidEan13));
    }

    @Test
    void parse_UpcA_ConvertsToEan13() {
        // Given - UPC-A code (12 digits)
        String upcA = "012345678905"; // Valid UPC-A
        
        // When
        BarcodeService.ParsedBarcode result = barcodeService.parse(upcA);

        // Then - Should convert to EAN-13 by prepending 0
        assertNotNull(result);
        assertEquals("0012345678905", result.gtin());
    }

    @Test
    void parse_EmptyString_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            barcodeService.parse(""));
        assertThrows(IllegalArgumentException.class, () -> 
            barcodeService.parse(null));
        assertThrows(IllegalArgumentException.class, () -> 
            barcodeService.parse("   "));
    }

    @Test
    void parse_Gs1DataMatrix_ExtractsMetadata() {
        // Given - GS1 DataMatrix format with AI (01)GTIN(17)EXPIRY(10)LOT(21)SERIAL
        String dataMatrix = "(01)07791234567897(17)251231(10)LOT123(21)SERIAL456";

        // When
        BarcodeService.ParsedBarcode result = barcodeService.parse(dataMatrix);

        // Then
        assertNotNull(result);
        assertEquals("07791234567897", result.gtin());
        assertTrue(result.isValid());
        assertEquals("LOT123", result.lotNumber().orElse(null));
        assertEquals("SERIAL456", result.serialNumber().orElse(null));
    }

    @Test
    void isValidEan13_ValidCodes() {
        // Test known valid EAN-13 codes
        assertTrue(barcodeService.isValidEan13("7791234567897"));
        assertTrue(barcodeService.isValidEan13("5449000000996"));
        assertTrue(barcodeService.isValidEan13("0012345678905")); // EAN-13 from UPC-A
    }

    @Test
    void isValidEan13_InvalidCodes() {
        // Test invalid EAN-13 codes (wrong length or checksum)
        assertFalse(barcodeService.isValidEan13("7791234567890")); // Wrong checksum
        assertFalse(barcodeService.isValidEan13("1234567890123")); // Wrong checksum
        assertFalse(barcodeService.isValidEan13("123456789012"));  // Too short
        assertFalse(barcodeService.isValidEan13("12345678901234")); // Too long
        assertFalse(barcodeService.isValidEan13("779123456789A")); // Non-numeric
    }

    @Test
    void parsedBarcode_RecordProperties() {
        // Given
        UUID batchId = UUID.randomUUID();
        BarcodeService.ParsedBarcode barcode = new BarcodeService.ParsedBarcode(
            "7791234567897", true, Optional.of("LOT123"), 
            Optional.of("2025-12-31"), Optional.of("SERIAL456"), 
            Optional.of(batchId)
        );

        // Then
        assertEquals("7791234567897", barcode.gtin());
        assertTrue(barcode.isValid());
        assertEquals("LOT123", barcode.lotNumber().orElse(null));
        assertEquals("2025-12-31", barcode.expiryDate().orElse(null));
        assertEquals("SERIAL456", barcode.serialNumber().orElse(null));
        assertEquals(batchId, barcode.batchId().orElse(null));
    }
}
