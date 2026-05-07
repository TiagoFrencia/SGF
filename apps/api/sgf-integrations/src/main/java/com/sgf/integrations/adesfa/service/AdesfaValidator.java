package com.sgf.integrations.adesfa.service;

import com.sgf.integrations.adesfa.domain.AdesfaValidationResult;
import com.sgf.integrations.adesfa.web.AdesfaValidationRequest;

/**
 * Interface for specific social security / health insurance validators.
 */
public interface AdesfaValidator {
    String getValidatorCode();
    AdesfaValidationResult validate(AdesfaValidationRequest request);
}
