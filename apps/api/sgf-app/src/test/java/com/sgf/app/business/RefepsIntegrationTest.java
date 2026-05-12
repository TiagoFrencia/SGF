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
class RefepsIntegrationTest extends PostgresIntegrationTestSupport {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.integrations.refeps.mode", () -> "mock");
    }

    @Autowired
    private SalesService salesService;

    @Test
    void shouldCompleteSaleWhenProfessionalIsHabilitado() {
        // Given: Un médico con matrícula válida
        SalesService.SaleRequest request = new SalesService.SaleRequest(
            "refeps-ok-" + UUID.randomUUID(),
            List.of(),
            "CASH",
            "20-12345678-9",
            null, null,
            "MAT-12345", // Doctor License
            "NAC"        // Region
        );

        // When
        SaleCompletedResponse response = salesService.create(request, "tiago_refeps");

        // Then
        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldAbortSaleWhenProfessionalIsSuspended() {
        // Given: Un médico con matrícula que empieza con 9 (rechazo en Mock)
        SalesService.SaleRequest request = new SalesService.SaleRequest(
            "refeps-fail-" + UUID.randomUUID(),
            List.of(),
            "CASH",
            "20-12345678-9",
            null, null,
            "999-SUSPENDED", // Licencia suspendida
            "NAC"
        );

        // When / Then
        assertThatThrownBy(() -> salesService.create(request, "tiago_refeps"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Profesional no habilitado en REFEPS");
    }
}
