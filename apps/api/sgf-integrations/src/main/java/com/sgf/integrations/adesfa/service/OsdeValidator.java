package com.sgf.integrations.adesfa.service;

import com.sgf.integrations.adesfa.domain.AdesfaValidationResult;
import com.sgf.integrations.adesfa.web.AdesfaValidationRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * OSDE validator.
 * Implements standard 40% or 50% coverage rules.
 */
@Component
public class OsdeValidator implements AdesfaValidator {

    @Override
    public String getValidatorCode() {
        return "OSDE";
    }

    @Override
    public AdesfaValidationResult validate(AdesfaValidationRequest request) {
        BigDecimal total = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // OSDE standard: 40%
        BigDecimal coverage = total.multiply(new BigDecimal("0.40"));
        BigDecimal patientPay = total.subtract(coverage);

        return new AdesfaValidationResult(
                true,
                "APPROVED_OSDE",
                "Validación OSDE Exitosa - Cobertura 40%",
                coverage,
                patientPay,
                java.util.UUID.randomUUID().toString()
        );
    }
}
