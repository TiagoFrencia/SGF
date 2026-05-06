package com.sgf.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchTransferRepository extends JpaRepository<BranchTransfer, UUID> {

    List<BranchTransfer> findBySourceBranchIdAndStatus(UUID sourceBranchId, BranchTransfer.TransferStatus status);

    List<BranchTransfer> findByDestinationBranchIdAndStatus(UUID destinationBranchId, BranchTransfer.TransferStatus status);

    Optional<BranchTransfer> findByIdAndStatus(UUID id, BranchTransfer.TransferStatus status);
}