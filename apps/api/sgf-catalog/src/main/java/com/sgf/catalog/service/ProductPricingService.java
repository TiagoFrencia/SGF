package com.sgf.catalog.service;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductPriceSnapshot;
import com.sgf.catalog.domain.ProductPriceSnapshotRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductPricingService {

    private final ProductPriceSnapshotRepository priceSnapshotRepository;

    public ProductPricingService(ProductPriceSnapshotRepository priceSnapshotRepository) {
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ProductPriceQuote> latestPrice(Product product) {
        return priceSnapshotRepository.findTopByProductOrderByEffectiveDateDescCreatedAtDesc(product)
                .map(this::toQuote);
    }

    private ProductPriceQuote toQuote(ProductPriceSnapshot snapshot) {
        return new ProductPriceQuote(
                snapshot.getRetailPrice(),
                snapshot.getPamiAffiliatePrice(),
                snapshot.getPamiDiscountCode(),
                snapshot.getPamiDiscountLabel(),
                snapshot.getSource(),
                snapshot.getEffectiveDate()
        );
    }
}
