package com.sgf.modules.integrations.adesfa.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdesfaValidationRepository extends JpaRepository<AdesfaValidation, UUID> {
    List<AdesfaValidation> findTop50ByOrderByValidatedAtDesc();
}
