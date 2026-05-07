package com.sgf.integrations.adesfa.service;

import com.sgf.integrations.adesfa.domain.AdesfaValidationResult;
import com.sgf.integrations.adesfa.web.AdesfaValidationRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Swiss Medical validator.
 */
@Component
public class SwissMedicalValidator implements AdesfaValidator {

    @Override
    public String getValidatorCode() {
        return "SWISS_MEDICAL";
    }

    @Override
    public AdesfaValidationResult validate(AdesfaValidationRequest request) {
        BigDecimal total = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Swiss Medical: 50%
        BigDecimal coverage = total.multiply(new BigDecimal("0.50"));
        BigDecimal patientPay = total.subtract(coverage);

        return new AdesfaValidationResult(
                true,
                "APPROVED_SWISS",
                "Validación Swiss Medical Exitosa - Cobertura 50%",
                coverage,
                patientPay,
                java.util.UUID.randomUUID().toString()
        );
    }
}
