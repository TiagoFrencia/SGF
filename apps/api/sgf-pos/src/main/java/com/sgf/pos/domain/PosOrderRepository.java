package com.sgf.pos.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosOrderRepository extends JpaRepository<PosOrder, UUID> {

    List<PosOrder> findByBranchIdAndStatus(UUID branchId, PosOrder.OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<PosOrder> findByIdAndStatus(UUID id, PosOrder.OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<PosOrder> findByBranchIdAndStatusIn(UUID branchId, List<PosOrder.OrderStatus> statuses);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<PosOrder> findById(UUID id);

    Optional<PosOrder> findTopByBranchIdAndStatusOrderByOrderNumberDesc(UUID branchId, PosOrder.OrderStatus status);
}
