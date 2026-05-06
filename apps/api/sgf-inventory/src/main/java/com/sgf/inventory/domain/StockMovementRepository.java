package com.sgf.inventory.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    @Query("""
            SELECT m FROM StockMovement m
            WHERE m.product.id = :productId
              AND m.movementType = 'OUT'
              AND m.occurredAt >= :since
            ORDER BY m.occurredAt ASC
            """)
    List<StockMovement> findOutMovementsSince(
            @Param("productId") UUID productId,
            @Param("since") OffsetDateTime since);
}