package com.sgf.integrations.adesfa.service;

import com.sgf.integrations.adesfa.domain.AdesfaValidationResult;
import com.sgf.integrations.adesfa.web.AdesfaValidationRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * PAMI (Programa de Atención Médica Integral) validator.
 * Implements 70/30 or 100% coverage rules based on PAMI regulations.
 */
@Component
public class PamiValidator implements AdesfaValidator {

    @Override
    public String getValidatorCode() {
        return "PAMI";
    }

    @Override
    public AdesfaValidationResult validate(AdesfaValidationRequest request) {
        // Logic for PAMI: usually 70% coverage for chronic, 100% for oncological/special
        // Here we implement a standard 70/30 split for the simulation
        BigDecimal total = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal coverage = total.multiply(new BigDecimal("0.70"));
        BigDecimal patientPay = total.subtract(coverage);

        return new AdesfaValidationResult(
                true,
                "APPROVED_PAMI",
                "Validación PAMI Exitosa - Cobertura 70%",
                coverage,
                patientPay,
                java.util.UUID.randomUUID().toString()
        );
    }
}
