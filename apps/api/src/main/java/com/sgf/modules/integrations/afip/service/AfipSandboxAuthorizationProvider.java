package com.sgf.modules.integrations.afip.service;

import com.sgf.modules.integrations.afip.domain.AfipInvoiceStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AfipSandboxAuthorizationProvider implements AfipAuthorizationProvider {

    @Override
    public AfipMode mode() {
        return AfipMode.SANDBOX;
    }

    @Override
    public AfipAuthorizationResult authorize(AfipAuthorizationCommand command) {
        String cae = String.format("%014d", Math.abs(command.saleId().hashCode()) + 100000000L);
        String responseJson = """
                {
                  "environment": "sandbox",
                  "message": "Simulated AFIP authorization",
                  "pointOfSale": %d,
                  "invoiceType": "%s"
                }
                """.formatted(command.pointOfSale(), command.invoiceType());
        return new AfipAuthorizationResult(
                AfipInvoiceStatus.AUTHORIZED,
                command.voucherNumberFrom(),
                command.voucherNumberTo(),
                "A",
                cae,
                LocalDate.now().plusDays(10),
                "sandbox-" + command.saleId(),
                responseJson,
                List.of(),
                List.of(),
                OffsetDateTime.now().plusHours(8)
        );
    }
}
