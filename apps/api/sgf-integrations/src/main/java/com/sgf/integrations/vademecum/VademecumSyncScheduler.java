package com.sgf.integrations.vademecum;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled synchronization of AlfaBeta and Kairos vademecums.
 *
 * Runs daily at 3:00 AM (low-traffic window) to fetch product catalog updates,
 * rebuild generic suggestion indexes, and update commercial data in the local catalog.
 *
 * Sync strategy:
 * 1. Page through AlfaBeta updates (100 products per page)
 * 2. Page through Kairos updates (50 products per page)
 * 3. Update local product records with vademecum codes
 * 4. Rebuild generic suggestion IFA index
 *
 * Handles rate limiting and partial failure gracefully (logs and continues).
 */
@Component
public class VademecumSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(VademecumSyncScheduler.class);

    private final AlfaBetaConnector alfaBetaConnector;
    private final KairosConnector kairosConnector;
    private final GenericSuggestionService genericSuggestionService;
    private final com.sgf.catalog.service.ProductService productService;

    public VademecumSyncScheduler(AlfaBetaConnector alfaBetaConnector,
                                   KairosConnector kairosConnector,
                                   GenericSuggestionService genericSuggestionService,
                                   com.sgf.catalog.service.ProductService productService) {
        this.alfaBetaConnector = alfaBetaConnector;
        this.kairosConnector = kairosConnector;
        this.genericSuggestionService = genericSuggestionService;
        this.productService = productService;
    }

    /**
     * Daily full sync at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void syncFormularies() {
        log.info("=== VADEMECUM DAILY SYNC STARTED ===");

        int alfaCount = syncAlfaBeta();
        int kairosCount = syncKairos();
        rebuildGenericIndex();

        log.info("=== VADEMECUM DAILY SYNC COMPLETED: AlfaBeta={}, Kairos={} ===",
                alfaCount, kairosCount);
    }

    /**
     * On-demand sync (manual trigger or startup).
     */
    public void syncNow() {
        log.info("Manual vademecum sync triggered");
        syncFormularies();
    }

    private int syncAlfaBeta() {
        int total = 0;
        int page = 0;
        int maxPages = 50; // Safety limit: 50 pages × 100 = 5000 products max per sync

        try {
            while (page < maxPages) {
                List<AlfaBetaConnector.AlfaBetaProduct> updates =
                        alfaBetaConnector.fetchDailyUpdates(page, 100);
                if (updates.isEmpty()) break;

                for (var product : updates) {
                    try {
                        Optional<com.sgf.catalog.domain.Product> existing =
                                productService.findByGtinOptional(product.gtin());
                        if (existing.isPresent()) {
                            // Update vademecum codes
                            productService.updateCommercialData(
                                    product.gtin(),
                                    "ALFABETA:" + product.gtin(),
                                    existing.get().getKairosCode()
                            );
                        }
                    } catch (Exception e) {
                        log.warn("Failed to update product {} from AlfaBeta: {}",
                                product.gtin(), e.getMessage());
                    }
                }
                total += updates.size();
                page++;

                // Rate limit: small delay between pages
                if (page % 5 == 0) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AlfaBeta sync interrupted");
        } catch (Exception e) {
            log.error("AlfaBeta sync failed: {}", e.getMessage());
        }

        log.info("AlfaBeta sync: {} products processed", total);
        return total;
    }

    private int syncKairos() {
        int total = 0;
        int offset = 0;
        int maxBatches = 100; // Safety limit: 100 × 50 = 5000 products

        try {
            while (offset < maxBatches * 50) {
                List<KairosConnector.KairosProduct> updates =
                        kairosConnector.fetchDailyUpdates(offset, 50);
                if (updates.isEmpty()) break;

                for (var product : updates) {
                    try {
                        Optional<com.sgf.catalog.domain.Product> existing =
                                productService.findByGtinOptional(product.gtin());
                        if (existing.isPresent()) {
                            productService.updateCommercialData(
                                    product.gtin(),
                                    existing.get().getAlfabetCode(),
                                    "KAIROS:" + product.gtin()
                            );
                        }
                    } catch (Exception e) {
                        log.warn("Failed to update product {} from Kairos: {}",
                                product.gtin(), e.getMessage());
                    }
                }
                total += updates.size();
                offset += 50;

                // Rate limit
                if (offset % 250 == 0) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Kairos sync interrupted");
        } catch (Exception e) {
            log.error("Kairos sync failed: {}", e.getMessage());
        }

        log.info("Kairos sync: {} products processed", total);
        return total;
    }

    /**
     * Rebuild the in-memory IFA → GTIN index for generic suggestions.
     * In production, this would query a dedicated DB table populated by the sync.
     */
    private void rebuildGenericIndex() {
        try {
            // Build index from AlfaBeta data by paging through all active ingredients
            var index = new java.util.concurrent.ConcurrentHashMap<String, java.util.List<String>>();

            int page = 0;
            while (page < 50) {
                List<AlfaBetaConnector.AlfaBetaProduct> products =
                        alfaBetaConnector.fetchDailyUpdates(page, 500);
                if (products.isEmpty()) break;

                for (var product : products) {
                    if (product.activeIngredient() != null) {
                        String ifa = product.activeIngredient().toUpperCase().trim();
                        index.computeIfAbsent(ifa, k -> new java.util.ArrayList<>())
                                .add(product.gtin());
                    }
                }
                page++;
            }

            genericSuggestionService.rebuildIndex(index);
            log.info("Generic index rebuilt: {} IFAs, {} products",
                    index.size(),
                    index.values().stream().mapToInt(java.util.List::size).sum());
        } catch (Exception e) {
            log.error("Failed to rebuild generic suggestion index: {}", e.getMessage());
        }
    }
}