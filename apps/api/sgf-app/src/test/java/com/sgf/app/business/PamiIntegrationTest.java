package com.sgf.app.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sgf.app.support.PostgresIntegrationTestSupport;
import com.sgf.pos.service.SalesService;
import com.sgf.pos.web.SaleCompletedResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Tag("integration")
class PamiIntegrationTest extends PostgresIntegrationTestSupport {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.integrations.pami.mode", () -> "mock");
    }

    @Autowired
    private SalesService salesService;

    @Test
    void shouldCompleteSaleWhenPamiApproves() {
        // Given: Una receta válida (no empieza con 9)
        SalesService.SaleRequest request = new SalesService.SaleRequest(
            "pami-ok-" + UUID.randomUUID(),
            List.of(),
            "CASH",
            "20-12345678-9",
            "REC-12345", // Prescription ID
            "BEN-999888", // Beneficiary ID
            null,
            null
        );

        // When
        SaleCompletedResponse response = salesService.create(request, "tiago_pami");

        // Then
        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldAbortSaleWhenPamiRejects() {
        // Given: Una receta que empieza con 9 (rechazo simulado en Mock)
        SalesService.SaleRequest request = new SalesService.SaleRequest(
            "pami-fail-" + UUID.randomUUID(),
            List.of(),
            "CASH",
            "20-12345678-9",
            "999-REJECT", // Esto disparará el rechazo en el Mock
            "BEN-999888",
            null,
            null
        );

        // When / Then
        assertThatThrownBy(() -> salesService.create(request, "tiago_pami"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Validación PAMI rechazada");
    }
}
