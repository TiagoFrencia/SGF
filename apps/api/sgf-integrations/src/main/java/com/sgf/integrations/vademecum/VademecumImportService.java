package com.sgf.integrations.vademecum;

import com.sgf.catalog.service.ProductService;
import com.sgf.catalog.service.VademecumProductImportCommand;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VademecumImportService {

    private static final Logger log = LoggerFactory.getLogger(VademecumImportService.class);

    private final PublicMsalVademecumProvider publicMsalProvider;
    private final ProductService productService;

    public VademecumImportService(PublicMsalVademecumProvider publicMsalProvider,
                                  ProductService productService) {
        this.publicMsalProvider = publicMsalProvider;
        this.productService = productService;
    }

    public ImportResult importPublicMsalSeeds(List<String> seeds) {
        Set<String> uniqueSeeds = new LinkedHashSet<>(seeds == null || seeds.isEmpty()
                ? List.of("ibuprofeno", "paracetamol", "amoxicilina", "omeprazol", "losartan")
                : seeds);
        int fetched = 0;
        int imported = 0;
        int failed = 0;

        for (String seed : uniqueSeeds) {
            List<VademecumProvider.VademecumProduct> products = publicMsalProvider.search(seed);
            fetched += products.size();
            for (var product : products) {
                try {
                    productService.importFromVademecum(toCommand(product));
                    imported++;
                } catch (Exception e) {
                    failed++;
                    log.warn("Failed to import public vademecum product {}: {}",
                            product.sourceRecordKey(), e.getMessage());
                }
            }
        }

        return new ImportResult(publicMsalProvider.providerCode(), uniqueSeeds.size(), fetched, imported, failed);
    }

    private VademecumProductImportCommand toCommand(VademecumProvider.VademecumProduct product) {
        return new VademecumProductImportCommand(
                publicMsalProvider.providerCode(),
                product.sourceRecordKey(),
                product.gtin(),
                product.troquel(),
                product.barcode(),
                product.commercialName(),
                product.presentation(),
                product.laboratory(),
                product.laboratoryCode(),
                product.activeIngredient(),
                product.snomedCode(),
                product.saleCondition(),
                product.pharmaceuticalForm(),
                product.unitsPerPackage(),
                product.retailPrice(),
                product.pamiAffiliatePrice(),
                product.pamiDiscountCode(),
                product.pamiDiscountLabel(),
                product.effectiveDate()
        );
    }

    public record ImportResult(String source, int seeds, int fetched, int imported, int failed) {
    }
}
