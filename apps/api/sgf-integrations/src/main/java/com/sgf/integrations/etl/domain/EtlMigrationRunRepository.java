package com.sgf.integrations.etl.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EtlMigrationRunRepository extends JpaRepository<EtlMigrationRun, UUID> {
    Optional<EtlMigrationRun> findByMigrationId(String migrationId);
}
