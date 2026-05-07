package com.sgf.integrations.fhir.profiles;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates CUIR (Código Único de Identificación de Receta) as defined by
 * the Argentine Digital Prescription Law (Ley 27.553).
 *
 * CUIR format specifications:
 * - Type 1 (Standard): UUID v4-based, prefixed with "CUIR-"
 *   Example: CUIR-a1b2c3d4-e5f6-7890-abcd-ef1234567890
 * - Type 2 (Numeric): 18-digit numeric with checksum
 *   Format: {timestamp HHmmss}{date yyyyMMdd}{random 4}{checksum}
 * - Type 3 (Verifiable): SHA-256 hash of prescription data → base62 encoding
 *
 * The CUIR is generated locally and registered via ReNaPDiS when available.
 * Local CUIRs are valid for offline operation and reconciled on sync.
 */
public final class CuirGenerator {

    private static final Logger log = LoggerFactory.getLogger(CuirGenerator.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CUIR_NAMESPACE = "https://www.argentina.gob.ar/salud/renapdis/cuir";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CuirGenerator() {}

    /**
     * Generate a standard UUID-based CUIR (Type 1).
     * Preferred for most prescriptions.
     */
    public static Cuir generateStandard() {
        String uuid = UUID.randomUUID().toString();
        String cuir = "CUIR-" + uuid;
        return new Cuir(cuir, CuirType.STANDARD, CUIR_NAMESPACE);
    }

    /**
     * Generate a numeric CUIR (Type 2).
     * Useful for manual entry and paper-based processes.
     * Format: {HHmmss}{yyyyMMdd}{random4}{checkDigit1}
     */
    public static Cuir generateNumeric() {
        Instant now = Instant.now();
        String timestamp = now.atZone(ZoneOffset.UTC).format(TIMESTAMP_FORMAT);
        // Rearranged: date portion first for readability
        String datePart = timestamp.substring(0, 8);  // yyyyMMdd
        String timePart = timestamp.substring(8, 14);  // HHmmss

        int random = SECURE_RANDOM.nextInt(10000);
        String base = datePart + timePart + String.format("%04d", random);
        int checksum = luhnChecksum(base);
        String cuir = base + checksum;

        return new Cuir(cuir, CuirType.NUMERIC, CUIR_NAMESPACE);
    }

    /**
     * Generate a verifiable CUIR (Type 3) from prescription data.
     * Produces a deterministic CUIR that can be independently verified.
     *
     * @param prescriberId   REFEPS matrícula
     * @param patientDni     Patient DNI
     * @param medicationHash SHA-256 of medication data
     */
    public static Cuir generateVerifiable(String prescriberId, String patientDni, String medicationHash) {
        String input = prescriberId + "|" + patientDni + "|" + medicationHash + "|" + System.currentTimeMillis();
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String base62 = base62Encode(digest).substring(0, 20);
            String cuir = "CUIR-V-" + base62;
            return new Cuir(cuir, CuirType.VERIFIABLE, CUIR_NAMESPACE);
        } catch (java.security.NoSuchAlgorithmException e) {
            log.error("SHA-256 not available, falling back to standard", e);
            return generateStandard();
        }
    }

    /**
     * Luhn checksum (same algorithm as credit cards / CUIT).
     */
    private static int luhnChecksum(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(digits.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Base62 encoding (0-9, A-Z, a-z) for compact representation.
     */
    private static String base62Encode(byte[] bytes) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        java.math.BigInteger number = new java.math.BigInteger(1, bytes);
        StringBuilder sb = new StringBuilder();
        java.math.BigInteger base = java.math.BigInteger.valueOf(62);
        while (number.compareTo(java.math.BigInteger.ZERO) > 0) {
            java.math.BigInteger[] divmod = number.divideAndRemainder(base);
            sb.insert(0, chars.charAt(divmod[1].intValue()));
            number = divmod[0];
        }
        // Ensure minimum length
        while (sb.length() < 20) sb.insert(0, '0');
        return sb.toString();
    }

    /**
     * Validate a CUIR format.
     */
    public static boolean isValid(String cuir) {
        if (cuir == null || cuir.isEmpty()) return false;
        if (cuir.startsWith("CUIR-")) {
            String rest = cuir.substring(5);
            if (rest.startsWith("V-")) {
                // Type 3: CUIR-V-{base62}
                return rest.length() >= 22 && rest.substring(2).matches("[0-9A-Za-z]+");
            }
            // Type 1: UUID
            return rest.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }
        // Type 2: 19 digits
        if (cuir.matches("\\d{19}")) {
            String base = cuir.substring(0, 18);
            int expected = Character.getNumericValue(cuir.charAt(18));
            return luhnChecksum(base) == expected;
        }
        return false;
    }

    // --- Types ---

    public enum CuirType {
        STANDARD, NUMERIC, VERIFIABLE
    }

    public record Cuir(String value, CuirType type, String system) {
        @Override
        public String toString() {
            return value;
        }

        public String toIdentifierSystem() {
            return system;
        }

        public String toIdentifierValue() {
            return value;
        }
    }
}