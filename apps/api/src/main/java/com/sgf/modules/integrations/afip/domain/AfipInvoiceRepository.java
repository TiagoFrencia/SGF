package com.sgf.modules.integrations.afip.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AfipInvoiceRepository extends JpaRepository<AfipInvoice, UUID> {
    Optional<AfipInvoice> findBySaleId(UUID saleId);
}

