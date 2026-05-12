package com.sgf.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchTransferRepository extends JpaRepository<BranchTransfer, UUID> {

    @EntityGraph(attributePaths = {"product", "batch"})
    List<BranchTransfer> findBySourceBranchId(UUID sourceBranchId);

    @EntityGraph(attributePaths = {"product", "batch"})
    List<BranchTransfer> findBySourceBranchIdAndStatus(UUID sourceBranchId, BranchTransfer.TransferStatus status);

    @EntityGraph(attributePaths = {"product", "batch"})
    List<BranchTransfer> findByDestinationBranchId(UUID destinationBranchId);

    @EntityGraph(attributePaths = {"product", "batch"})
    List<BranchTransfer> findByDestinationBranchIdAndStatus(UUID destinationBranchId, BranchTransfer.TransferStatus status);

    @EntityGraph(attributePaths = {"product", "batch"})
    Optional<BranchTransfer> findByIdAndStatus(UUID id, BranchTransfer.TransferStatus status);

    @Override
    @EntityGraph(attributePaths = {"product", "batch"})
    Optional<BranchTransfer> findById(UUID id);
}
