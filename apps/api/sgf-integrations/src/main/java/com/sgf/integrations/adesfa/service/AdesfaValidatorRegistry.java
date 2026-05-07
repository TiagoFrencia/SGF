package com.sgf.integrations.adesfa.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdesfaValidatorRegistry {

    private final Map<String, AdesfaValidator> validators;

    public AdesfaValidatorRegistry(List<AdesfaValidator> validatorList) {
        this.validators = validatorList.stream()
                .collect(Collectors.toMap(
                        AdesfaValidator::getValidatorCode,
                        v -> v
                ));
    }

    public Optional<AdesfaValidator> getValidator(String code) {
        return Optional.ofNullable(validators.get(code));
    }
}
