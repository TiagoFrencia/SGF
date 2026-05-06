package com.sgf.integrations.anmat.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AnmatRemediationCaseRepository extends JpaRepository<AnmatRemediationCase, UUID>, JpaSpecificationExecutor<AnmatRemediationCase> {
    Optional<AnmatRemediationCase> findByGtinAndSerialNumberAndIssueCode(String gtin, String serialNumber, String issueCode);
}
