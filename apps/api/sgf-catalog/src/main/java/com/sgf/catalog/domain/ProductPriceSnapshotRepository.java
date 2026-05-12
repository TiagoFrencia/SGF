package com.sgf.catalog.domain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceSnapshotRepository extends JpaRepository<ProductPriceSnapshot, UUID> {
    Optional<ProductPriceSnapshot> findByProductAndSourceAndSourceRecordKeyAndEffectiveDate(
            Product product, String source, String sourceRecordKey, LocalDate effectiveDate);

    Optional<ProductPriceSnapshot> findTopByProductOrderByEffectiveDateDescCreatedAtDesc(Product product);
}
