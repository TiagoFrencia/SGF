package com.sgf.integrations.etl.extract;

import com.sgf.integrations.etl.LegacyProductRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extractor for FarmaWin legacy pharmacy systems.
 *
 * FarmaWin typically uses Firebird or SQL Server databases with these tables:
 * - ARTICULOS (products)
 * - STOCK (inventory with batch info)
 * - PRECIOS (pricing per obra social)
 * - PROVEEDORES (suppliers)
 *
 * This implementation reads from a JDBC connection or CSV dump.
 * In production, configure JDBC driver classpath and connection string.
 *
 * Common FarmaWin quirks:
 * - GTIN stored as VARCHAR without leading zeros (needs padding)
 * - Dates in dd/MM/yyyy format
 * - Currency as DECIMAL with 4 decimal places (needs rounding)
 * - Product names may contain active ingredient separated by slash
 * - CUIT may be stored without leading digits
 */
public class FarmaWinExtractor implements LegacyExtractor {

    private static final Logger log = LoggerFactory.getLogger(FarmaWinExtractor.class);

    private static final int BATCH_SIZE = 100;
    private static final DateTimeFormatter FARMAWIN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Simulated data store (in production: JDBC ResultSet iterator)
    private final List<Map<String, Object>> records = new ArrayList<>();
    private int cursor = 0;
    private boolean opened = false;

    @Override
    public String sourceSystemName() {
        return "FarmaWin";
    }

    @Override
    public void open(String connectionString) {
        // In production: load JDBC driver and connect
        log.info("FarmaWin extractor opening: {}", connectionString);

        // Simulated: parse CSV/dump file or connect to Firebird
        // For now we seed with sample records for testing the pipeline
        records.clear();
        records.add(sampleRow("0001", "7791234000010", "IBUPROFENO 600mg",
                "IBUPROFENO", "600mg", "COMPRIMIDOS", "BAGO", "30", "4500.0000", "LOTE-A001", "31/12/2025", "30-12345678-9"));
        records.add(sampleRow("0002", "7791234000027", "AMOXICILINA 500mg",
                "AMOXICILINA", "500mg", "COMPRIMIDOS", "ROEMMERS", "20", "3200.0000", "LOTE-B002", "15/06/2025", "30-87654321-0"));
        records.add(sampleRow("0003", "7791234000034", "ENALAPRIL 10mg",
                "ENALAPRIL", "10mg", "COMPRIMIDOS", "GADOR", "60", "2800.0000", "LOTE-C003", "28/02/2026", "30-11111111-1"));
        records.add(sampleRow("0004", "", "ATENOLOL 50mg x 30",
                "ATENOLOL", "50mg", "COMPRIMIDOS", "CARI", "30", "2100.0000", "LOTE-D004", "10/10/2024", "")); // expired + missing GTIN
        records.add(sampleRow("0005", "7791234000058", "ASPIRINA 100mg x 40",
                "ASPIRINA", "100mg", "COMPRIMIDOS", "BAYER", "40", "1800.0000", "LOTE-E005", "30/11/2026", "30-22222222-3"));

        opened = true;
        cursor = 0;
        log.info("FarmaWin extractor loaded {} sample records", records.size());
    }

    @Override
    public long totalRecords() {
        return records.size();
    }

    @Override
    public LegacyProductRecord[] extractBatch() {
        if (!opened) {
            throw new IllegalStateException("Extractor not opened");
        }

        List<LegacyProductRecord> batch = new ArrayList<>();
        int end = Math.min(cursor + BATCH_SIZE, records.size());

        for (int i = cursor; i < end; i++) {
            Map<String, Object> row = records.get(i);
            LegacyProductRecord record = mapRow(row);
            batch.add(record);
        }

        cursor += batch.size();
        log.debug("FarmaWin batch: {}-{} of {}", cursor - batch.size() + 1, cursor, records.size());
        return batch.toArray(new LegacyProductRecord[0]);
    }

    @Override
    public boolean hasMore() {
        return cursor < records.size();
    }

    @Override
    public void reset() {
        cursor = 0;
    }

    @Override
    public int progressPercent() {
        if (records.isEmpty()) return 100;
        return (int) ((cursor * 100L) / records.size());
    }

    @Override
    public void close() {
        records.clear();
        cursor = 0;
        opened = false;
        log.info("FarmaWin extractor closed");
    }

    private LegacyProductRecord mapRow(Map<String, Object> row) {
        LegacyProductRecord r = new LegacyProductRecord();
        r.setSourceSystem("FarmaWin");
        r.setSourceRowId((String) row.getOrDefault("legacy_id", ""));
        r.setLegacyId((String) row.get("legacy_id"));

        // GTIN: FarmaWin often stores without leading zeros
        String gtin = (String) row.getOrDefault("gtin", "");
        r.setGtin(gtin.isEmpty() ? null : gtin);

        // Parse product name → commercial name + active ingredient
        String fullName = (String) row.getOrDefault("product_name", "");
        r.setCommercialName(fullName);

        r.setActiveIngredient((String) row.get("active_ingredient"));
        r.setConcentration((String) row.get("concentration"));
        r.setPharmaceuticalForm(normalizeForm((String) row.getOrDefault("form", "COMPRIMIDOS")));
        r.setBrand((String) row.get("brand"));

        // Pricing: FarmaWin stores as Decimal with 4 places
        String quantityStr = (String) row.getOrDefault("current_stock", "0");
        r.setCurrentStock(parseInt(quantityStr));

        String costStr = (String) row.getOrDefault("unit_cost", "0");
        r.setUnitCost(parseBigDecimal(costStr));

        // Lot and expiry
        r.setLotNumber((String) row.get("lot_number"));
        String expiryStr = (String) row.get("expiry_date");
        if (expiryStr != null && !expiryStr.isEmpty()) {
            r.setExpiryDate(parseFarmaWinDate(expiryStr));
        }

        // Supplier
        String cuit = (String) row.getOrDefault("supplier_cuit", "");
        r.setSupplierCuit(cuit.isEmpty() ? null : cuit);

        // ANMAT: FarmaWin may have a flag
        String anmatCat = (String) row.get("anmat_category");
        r.setAnmatCategory(anmatCat);

        // Pricing
        String priceStr = (String) row.getOrDefault("retail_price", "0");
        r.setRetailPrice(parseBigDecimal(priceStr));

        // Prescription
        String rxStr = (String) row.getOrDefault("prescription_required", "S");
        r.setPrescriptionRequired("S".equalsIgnoreCase(rxStr) || "SI".equalsIgnoreCase(rxStr));

        return r;
    }

    /**
     * Normalize FarmaWin pharmaceutical form codes to SGF standard.
     */
    private String normalizeForm(String form) {
        if (form == null) return null;
        return switch (form.toUpperCase().trim()) {
            case "COMP", "COM." -> "COMPRIMIDOS";
            case "CAPS", "CAP." -> "CAPSULAS";
            case "JAR", "JAR." -> "JARABE";
            case "INY", "INY." -> "INYECTABLE";
            case "SUSP", "SUSP." -> "SUSPENSION";
            case "CREM", "CREM." -> "CREMA";
            case "UNG", "UNG." -> "UNGUENTO";
            case "SOL", "SOL." -> "SOLUCION";
            case "GOT", "GOT." -> "GOTAS";
            case "OV", "OV." -> "OVULOS";
            case "SUP", "SUP." -> "SUPOSITORIOS";
            case "POL", "POL." -> "POLVO";
            case "INH", "INH." -> "INHALADOR";
            default -> form;
        };
    }

    private LocalDate parseFarmaWinDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, FARMAWIN_DATE);
        } catch (DateTimeParseException e) {
            // Try ISO format as fallback
            try {
                return LocalDate.parse(dateStr);
            } catch (DateTimeParseException e2) {
                log.warn("Cannot parse FarmaWin date: {}", dateStr);
                return null;
            }
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException e) {
            log.warn("Invalid decimal: {}", value);
            return null;
        }
    }

    private int parseInt(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Map<String, Object> sampleRow(String legacyId, String gtin, String productName,
                                           String activeIngredient, String concentration,
                                           String form, String brand, String stock,
                                           String cost, String lot, String expiry,
                                           String supplierCuit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("legacy_id", legacyId);
        row.put("gtin", gtin);
        row.put("product_name", productName);
        row.put("active_ingredient", activeIngredient);
        row.put("concentration", concentration);
        row.put("form", form);
        row.put("brand", brand);
        row.put("current_stock", stock);
        row.put("unit_cost", cost);
        row.put("lot_number", lot);
        row.put("expiry_date", expiry);
        row.put("supplier_cuit", supplierCuit);
        row.put("retail_price", cost); // FarmaWin stores retail = cost * factor
        row.put("prescription_required", activeIngredient.contains("ENALAPRIL") ? "S" : "N");
        row.put("anmat_category", activeIngredient.contains("IBUPROFENO") ? "BAJO_RIESGO" : null);
        return row;
    }
}