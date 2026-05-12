package com.sgf.app.support;

import com.sgf.catalog.domain.Product;
import com.sgf.inventory.domain.Batch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class IntegrationFixtures {

    private IntegrationFixtures() {
    }

    public static Product validProduct(String gtin, String sku, String commercialName) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setGtin(gtin);
        product.setSku(sku);
        product.setCommercialName(commercialName);
        product.setBrand("SGF Labs");
        product.setActiveIngredient(commercialName);
        product.setPrescriptionRequired(false);
        product.setRequiresTraceability(false);
        return product;
    }

    public static Batch validBatch(Product product, String lotNumber, int availableQuantity, BigDecimal unitCost) {
        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());
        batch.setProduct(product);
        batch.setLotNumber(lotNumber);
        batch.setExpiresAt(LocalDate.now().plusYears(2));
        batch.setAvailableQuantity(availableQuantity);
        batch.setUnitCost(unitCost);
        return batch;
    }
}
