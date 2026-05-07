package com.sgf.pos.service;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Barcode parsing and validation service for POS scanning.
 *
 * Supported formats:
 * <ul>
 *   <li><b>EAN-13</b> — Standard retail barcode (13 digits), the most common in Argentina</li>
 *   <li><b>GTIN-14</b> — Extended Global Trade Item Number (14 digits, packaging levels)</li>
 *   <li><b>UPC-A</b> — North American standard (12 digits), converted to EAN-13 for compatibility</li>
 *   <li><b>DataMatrix ANMAT</b> — Pharmaceutical traceability (GS1 Application Identifiers: 01=GTIN, 17=expiry, 10=lot, 21=serial)</li>
 * </ul>
 *
 * The service normalizes all scanned codes to a GTIN-compatible format for product lookup.
 */
@Service
public class BarcodeService {

    private static final Logger log = LoggerFactory.getLogger(BarcodeService.class);

    /**
     * Parse a scanned barcode and extract the GTIN for product lookup.
     *
     * @param rawScan Raw scanned string from the barcode reader
     * @return ParsedBarcode with product GTIN and optional metadata (lot, serial, expiry)
     */
    public ParsedBarcode parse(String rawScan) {
        if (rawScan == null || rawScan.isBlank()) {
            throw new IllegalArgumentException("Empty barcode scan");
        }

        String code = rawScan.trim();

        // Remove any framing characters (some scanners add STX/ETX or CR/LF)
        code = code.replaceAll("[^\\x20-\\x7E]", "");

        // DataMatrix ANMAT format: GS1 Application Identifier encoded
        // Format: (01)GTIN(17)EXPIRY(10)LOT(21)SERIAL
        if (code.startsWith("(01)") || code.startsWith("]d2")) {
            return parseGs1Ais(code);
        }

        // EAN-13: 13 digits
        if (code.matches("\\d{13}")) {
            if (!isValidEan13(code)) {
                log.warn("Invalid EAN-13 checksum: {}", code);
            }
            return new ParsedBarcode(code, null, null, null,
                    code.length() == 13 ? BarcodeType.EAN_13 : BarcodeType.UNKNOWN);
        }

        // GTIN-14: 14 digits (indicator digit + EAN-13 without check + new check digit)
        if (code.matches("\\d{14}")) {
            String gtin14 = normalizeGtin14(code);
            return new ParsedBarcode(gtin14, null, null, null, BarcodeType.GTIN_14);
        }

        // UPC-A: 12 digits → convert to EAN-13 by prepending "0"
        if (code.matches("\\d{12}")) {
            return new ParsedBarcode(normalizeUpca(code), null, null, null, BarcodeType.UPC_A);
        }

        // UPC-E: 8 digits compressed → expand to UPC-A then to EAN-13
        if (code.matches("\\d{8}") && (code.startsWith("0") || code.startsWith("1"))) {
            return new ParsedBarcode(expandUpce(code), null, null, null, BarcodeType.UPC_E);
        }

        // EAN-8: 8 digits for small packages
        if (code.matches("\\d{8}")) {
            return new ParsedBarcode(code, null, null, null, BarcodeType.EAN_8);
        }

        // Fallback: treat as raw internal code
        log.debug("Unrecognized barcode format, using as raw: {}", code);
        return new ParsedBarcode(code, null, null, null, BarcodeType.RAW);
    }

    /**
     * Parse GS1 Application Identifier format used by ANMAT DataMatrix.
     * Example: (01)07791234567890(17)250630(10)ABC123(21)000001
     */
    private ParsedBarcode parseGs1Ais(String code) {
        // Strip leading ]d2 or similar AI prefix if present
        String cleaned = code.replaceAll("^]d\\d", "");

        String gtin = null;
        String expiry = null;
        String lot = null;
        String serial = null;
        int pos = 0;

        while (pos < cleaned.length()) {
            if (cleaned.charAt(pos) != '(') {
                break;
            }
            int aiEnd = cleaned.indexOf(')', pos);
            if (aiEnd == -1) break;

            String ai = cleaned.substring(pos + 1, aiEnd);
            pos = aiEnd + 1;

            int valueLen = switch (ai) {
                case "01" -> 14; // GTIN
                case "17" -> 6;  // Expiry YYMMDD
                case "10" -> {    // Lot (variable, stop at next AI separator)
                    int nextAi = cleaned.indexOf("(", pos);
                    yield nextAi == -1 ? cleaned.length() - pos : nextAi - pos;
                }
                case "21" -> {    // Serial (variable)
                    int nextAi = cleaned.indexOf("(", pos);
                    yield nextAi == -1 ? cleaned.length() - pos : nextAi - pos;
                }
                default -> 0;
            };

            if (pos + valueLen > cleaned.length()) {
                break;
            }
            String value = cleaned.substring(pos, pos + valueLen);
            pos += valueLen;

            switch (ai) {
                case "01" -> gtin = value;
                case "17" -> expiry = formatExpiry(value);
                case "10" -> lot = value;
                case "21" -> serial = value;
            }
        }

        if (gtin == null) {
            log.warn("DataMatrix missing GTIN (AI 01): {}", code);
            return new ParsedBarcode(code, expiry, lot, serial, BarcodeType.DATAMATRIX_ANMAT);
        }

        return new ParsedBarcode(gtin, expiry, lot, serial, BarcodeType.DATAMATRIX_ANMAT);
    }

    /**
     * Validate EAN-13 checksum digit.
     */
    public boolean isValidEan13(String ean) {
        if (ean == null || ean.length() != 13 || !ean.matches("\\d+")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(ean.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int check = (10 - (sum % 10)) % 10;
        return check == Character.getNumericValue(ean.charAt(12));
    }

    /**
     * Normalize UPC-A (12 digits) to EAN-13 by prepending "0".
     */
    private String normalizeUpca(String upc) {
        return "0" + upc;
    }

    /**
     * Expand UPC-E (8 digits) to UPC-A (12 digits), then to EAN-13.
     * UPC-E is a zero-suppressed version of UPC-A.
     */
    private String expandUpce(String upce) {
        // UPC-E to UPC-A expansion rules
        if (upce.length() != 8) return upce;
        String manufacturer;
        String product;
        char last = upce.charAt(6);

        switch (last) {
            case '0', '1', '2' -> {
                manufacturer = upce.substring(0, 2) + last + "00";
                product = "00" + upce.substring(2, 6);
            }
            case '3' -> {
                manufacturer = upce.substring(0, 3) + "00";
                product = "000" + upce.substring(3, 6);
            }
            case '4' -> {
                manufacturer = upce.substring(0, 4) + "0";
                product = "0000" + upce.substring(4, 6);
            }
            default -> {
                manufacturer = upce.substring(0, 5) + "0";
                product = "0000" + last + upce.substring(5, 6);
            }
        }
        String upca = manufacturer + product + upce.charAt(7);
        return "0" + upca; // to EAN-13
    }

    /**
     * Normalize GTIN-14: ensure 14 digits.
     */
    private String normalizeGtin14(String code) {
        if (code.length() == 14) return code;
        if (code.length() == 13) return "0" + code;
        return code;
    }

    /**
     * Convert GS1 expiry (YYMMDD) to YYYY-MM-DD.
     */
    private String formatExpiry(String yyMMdd) {
        if (yyMMdd == null || yyMMdd.length() != 6) return yyMMdd;
        int year = Integer.parseInt(yyMMdd.substring(0, 2));
        return "20" + year + "-" + yyMMdd.substring(2, 4) + "-" + yyMMdd.substring(4, 6);
    }

    /**
     * Result of barcode parsing, ready for product lookup.
     */
    public record ParsedBarcode(
            String gtin,
            String expiry,
            String lotNumber,
            String serial,
            BarcodeType type
    ) {
        public boolean hasBatchInfo() {
            return lotNumber != null || expiry != null || serial != null;
        }


    }

    public enum BarcodeType {
        EAN_13,
        EAN_8,
        GTIN_14,
        UPC_A,
        UPC_E,
        DATAMATRIX_ANMAT,
        RAW,
        UNKNOWN
    }
}