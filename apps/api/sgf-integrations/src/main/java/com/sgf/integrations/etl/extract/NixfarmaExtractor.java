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
 * Extractor for Nixfarma legacy pharmacy systems.
 *
 * Nixfarma is a PostgreSQL-based system common in Argentine pharmacies.
 * Tables:
 * - nf_productos (products with commercial + therapeutic data)
 * - nf_lotes (batches with stock per branch)
 * - nf_precios (pricing by obra social and branch)
 * - nf_proveedores (suppliers)
 *
 * Nixfarma quirks:
 * - GTIN stored as BIGINT (loses leading zeros on export)
 * - Active ingredient stored in a separate table (nf_principios)
 * - Batch stock is per-branch (nf_stock_sucursal)
 * - Dates in yyyy-MM-dd format (PostgreSQL standard)
 * - Decimal prices with comma-separated currency
 * - Product codes use alphanumeric format: NXF-000001
 */
public class NixfarmaExtractor implements LegacyExtractor {

    private static final Logger log = LoggerFactory.getLogger(NixfarmaExtractor.class);

    private static final int BATCH_SIZE = 100;

    private final List<Map<String, Object>> records = new ArrayList<>();
    private int cursor = 0;
    private boolean opened = false;

    @Override
    public String sourceSystemName() {
        return "Nixfarma";
    }

    @Override
    public void open(String connectionString) {
        log.info("Nixfarma extractor opening: {}", connectionString);
        records.clear();

        // Nixfarma sample data — simulates PostgreSQL dump
        records.add(nxRow("NXF-00001", "07791234000010", "OMEPRAZOL 20mg x 28", "OMEPRAZOL",
                "20mg", "COMPRIMIDOS", "28", "3500.00", "LOTE-N01", "2026-03-15",
                "LABORATORIO BAGO", "30-12345678-9", "S"));
        records.add(nxRow("NXF-00002", "07791234000027", "LOSARTAN 50mg x 30", "LOSARTAN",
                "50mg", "COMPRIMIDOS", "30", "2800.00", "LOTE-N02", "2026-01-20",
                "ROEMMERS", "30-87654321-0", "S"));
        records.add(nxRow("NXF-00003", "07791234000034", "LEVOTIROXINA 100mcg x 50", "LEVOTIROXINA",
                "100mcg", "COMPRIMIDOS", "50", "4200.00", "LOTE-N03", "2025-11-30",
                "GLAXOSMITHKLINE", "30-11111111-1", "S"));
        records.add(nxRow("NXF-00004", "", "DICLOFENAC GEL 100g", "DICLOFENAC",
                "1%", "GEL", "100", "1800.00", "LOTE-N04", "2024-08-10",
                "NOVARTIS", "", "N")); // expired, no GTIN, no supplier CUIT
        records.add(nxRow("NXF-00005", "07791234000058", "METFORMINA 850mg x 60", "METFORMINA",
                "850mg", "COMPRIMIDOS", "60", "2200.00", "LOTE-N05", "2026-07-01",
                "GADOR", "30-22222222-3", "S"));

        opened = true;
        cursor = 0;
        log.info("Nixfarma extractor loaded {} sample records", records.size());
    }

    @Override
    public long totalRecords() {
        return records.size();
    }

    @Override
    public LegacyProductRecord[] extractBatch() {
        if (!opened) throw new IllegalStateException("Extractor not opened");

        List<LegacyProductRecord> batch = new ArrayList<>();
        int end = Math.min(cursor + BATCH_SIZE, records.size());

        for (int i = cursor; i < end; i++) {
            Map<String, Object> row = records.get(i);
            LegacyProductRecord record = mapRow(row);
            batch.add(record);
        }
        cursor += batch.size();
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
        log.info("Nixfarma extractor closed");
    }

    private LegacyProductRecord mapRow(Map<String, Object> row) {
        LegacyProductRecord r = new LegacyProductRecord();
        r.setSourceSystem("Nixfarma");
        r.setSourceRowId((String) row.getOrDefault("product_code", ""));
        r.setLegacyId((String) row.get("product_code"));

        // Nixfarma GTIN: BIGINT → string, may lose leading zero
        String gtin = (String) row.getOrDefault("gtin", "");
        if (gtin != null && !gtin.isEmpty()) {
            // Pad to 14 digits if numeric
            if (gtin.matches("\\d+") && gtin.length() < 14) {
                gtin = String.format("%014d", Long.parseLong(gtin));
            }
        } else {
            gtin = null;
        }
        r.setGtin(gtin);

        r.setCommercialName((String) row.getOrDefault("product_name", ""));
        r.setActiveIngredient((String) row.get("active_ingredient"));
        r.setConcentration((String) row.get("concentration"));
        r.setPharmaceuticalForm(normalizeForm((String) row.getOrDefault("form", "COMPRIMIDOS")));
        r.setBrand((String) row.get("brand"));

        String stockStr = (String) row.getOrDefault("current_stock", "0");
        r.setCurrentStock(parseInt(stockStr));

        String costStr = (String) row.getOrDefault("unit_cost", "0");
        r.setUnitCost(strToDecimal(costStr));

        r.setLotNumber((String) row.get("lot_number"));
        String expiryStr = (String) row.get("expiry_date");
        if (expiryStr != null && !expiryStr.isEmpty()) {
            try {
                r.setExpiryDate(LocalDate.parse(expiryStr));
            } catch (DateTimeParseException e) {
                log.warn("Cannot parse Nixfarma date: {}", expiryStr);
            }
        }

        String cuit = (String) row.getOrDefault("supplier_cuit", "");
        r.setSupplierCuit(cuit.isEmpty() ? null : cuit);

        String rxStr = (String) row.getOrDefault("prescription_required", "N");
        r.setPrescriptionRequired("S".equalsIgnoreCase(rxStr));

        String priceStr = (String) row.getOrDefault("retail_price", "0");
        r.setRetailPrice(strToDecimal(priceStr));

        // Nixfarma stores therapeutic category
        if (row.get("therapeutic_category") != null) {
            r.setTherapeuticCategory((String) row.get("therapeutic_category"));
        }

        return r;
    }

    private String normalizeForm(String form) {
        if (form == null) return null;
        return switch (form.toUpperCase().trim()) {
            case "COM", "COMP", "TAB", "TABLETA" -> "COMPRIMIDOS";
            case "CAP" -> "CAPSULAS";
            case "JBE", "JAR" -> "JARABE";
            case "INY", "AMP", "AMPOLLA" -> "INYECTABLE";
            case "SUSP" -> "SUSPENSION";
            case "CREMA" -> "CREMA";
            case "UNG" -> "UNGUENTO";
            case "SOL" -> "SOLUCION";
            case "GOTAS", "GTS" -> "GOTAS";
            case "OV" -> "OVULOS";
            case "SUP" -> "SUPOSITORIOS";
            case "POLVO" -> "POLVO";
            case "INH", "AER" -> "INHALADOR";
            case "GEL" -> "GEL";
            default -> form;
        };
    }

    private BigDecimal strToDecimal(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException e) {
            log.warn("Invalid Nixfarma decimal: {}", value);
            return null;
        }
    }

    private int parseInt(String value) {
        if (value == null || value.isEmpty()) return 0;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { return 0; }
    }

    private Map<String, Object> nxRow(String productCode, String gtin, String productName,
                                       String activeIngredient, String concentration,
                                       String form, String stock, String cost,
                                       String lot, String expiry, String brand,
                                       String supplierCuit, String rx) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("product_code", productCode);
        row.put("gtin", gtin);
        row.put("product_name", productName);
        row.put("active_ingredient", activeIngredient);
        row.put("concentration", concentration);
        row.put("form", form);
        row.put("current_stock", stock);
        row.put("unit_cost", cost);
        row.put("lot_number", lot);
        row.put("expiry_date", expiry);
        row.put("brand", brand);
        row.put("supplier_cuit", supplierCuit);
        row.put("retail_price", cost);
        row.put("prescription_required", rx);
        row.put("source_table", "nf_productos");
        return row;
    }
}