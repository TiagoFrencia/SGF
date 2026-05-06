package com.sgf.modules.sales.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
    Optional<Sale> findByExternalIdempotencyKey(String key);
}

