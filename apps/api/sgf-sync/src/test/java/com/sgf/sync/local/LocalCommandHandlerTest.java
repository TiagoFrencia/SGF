package com.sgf.sync.local;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LocalCommandHandlerTest {

    @Test
    void productAndSaleCommandsPersistLocallyAndEnqueueForSync() throws Exception {
        Path dbFile = Files.createTempFile("sgf-sync-test", ".db");
        LocalDatabase localDatabase = new LocalDatabase("jdbc:sqlite:" + dbFile);
        localDatabase.initialize();

        LocalSyncQueue localSyncQueue = new LocalSyncQueue(localDatabase, new ObjectMapper());
        LocalCommandHandler handler = new LocalCommandHandler(localDatabase, localSyncQueue, new ObjectMapper());

        handler.handle(new LocalCommandHandler.WriteCommand(
                "PRODUCT",
                "prod-1",
                "PRODUCT_CREATED",
                Map.of(
                        "gtin", "7791234567890",
                        "sku", "SKU-001",
                        "commercialName", "Ibuprofeno 600",
                        "brand", "SGF",
                        "activeIngredient", "Ibuprofeno",
                        "prescriptionRequired", false
                )
        ));

        handler.handle(new LocalCommandHandler.WriteCommand(
                "SALE",
                "sale-1",
                "SALE_COMPLETED",
                Map.of(
                        "externalIdempotencyKey", "offline-sale-1",
                        "totalAmount", 14500.00,
                        "status", "COMPLETED",
                        "soldAt", OffsetDateTime.now().toString()
                )
        ));

        try (Connection conn = localDatabase.getConnection(); Statement st = conn.createStatement()) {
            assertEquals(1, singleInt(st.executeQuery("SELECT COUNT(*) FROM local_products")));
            assertEquals(1, singleInt(st.executeQuery("SELECT COUNT(*) FROM local_sales")));
            assertEquals(2, singleInt(st.executeQuery("SELECT COUNT(*) FROM local_sync_queue")));
            assertEquals(2, singleInt(st.executeQuery("SELECT COUNT(*) FROM local_sync_queue WHERE status = 'PENDING'")));
        }
    }

    private int singleInt(ResultSet rs) throws Exception {
        rs.next();
        return rs.getInt(1);
    }
}
