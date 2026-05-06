package com.sgf.inventory.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Optional<Batch> findByProductIdAndLotNumber(UUID productId, String lotNumber);
    List<Batch> findByProductIdAndAvailableQuantityGreaterThanAndExpiresAtGreaterThanEqualOrderByExpiresAtAsc(
            UUID productId, Integer quantity, LocalDate expiresAt);
    List<Batch> findByExpiresAtBetweenAndAvailableQuantityGreaterThan(
            LocalDate from, LocalDate to, int minQuantity);
    List<Batch> findByProductIdAndAvailableQuantityGreaterThan(UUID productId, int minQuantity);
}

