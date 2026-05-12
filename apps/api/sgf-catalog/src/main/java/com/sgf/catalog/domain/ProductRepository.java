package com.sgf.catalog.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByGtinOrSku(String gtin, String sku);
    Optional<Product> findByGtin(String gtin);
    Optional<Product> findByTroquel(String troquel);
    java.util.List<Product> findByCommercialNameContainingIgnoreCase(String name);
    java.util.List<Product> findByActiveIngredientIgnoreCase(String activeIngredient);

    @Query("""
            select p from Product p
            join p.presentations pr
            where lower(p.commercialName) = lower(:commercialName)
              and lower(coalesce(pr.description, '')) = lower(:presentation)
              and lower(coalesce(p.laboratory, '')) = lower(:laboratory)
            """)
    Optional<Product> findByCommercialNamePresentationAndLaboratory(
            @Param("commercialName") String commercialName,
            @Param("presentation") String presentation,
            @Param("laboratory") String laboratory);
}
