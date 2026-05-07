package com.sgf.catalog.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByGtinOrSku(String gtin, String sku);
    Optional<Product> findByGtin(String gtin);
    java.util.List<Product> findByCommercialNameContainingIgnoreCase(String name);
    java.util.List<Product> findByActiveIngredientIgnoreCase(String activeIngredient);
}

