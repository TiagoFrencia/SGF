package com.sgf.app.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sgf.integrations.pami.service.PamiSiafarService;
import com.sgf.integrations.refeps.service.RefepsService;
import com.sgf.inventory.service.InventoryService;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleRepository;
import com.sgf.pos.service.SalesService;
import com.sgf.pos.web.SaleCompletedResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PamiSiafarService pamiService;

    @Mock
    private RefepsService refepsService;

    @InjectMocks
    private SalesService salesService;

    private void stubIdempotencyMissAndSave() {
        when(saleRepository.findByExternalIdempotencyKey(any())).thenReturn(Optional.empty());
        when(saleRepository.save(any())).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            if (sale.getId() == null) {
                sale.setId(UUID.randomUUID());
            }
            return sale;
        });
    }

    @Test
    void shouldProcessCashPaymentSuccessfully() {
        stubIdempotencyMissAndSave();

        // Given
        SalesService.SaleRequest request = new SalesService.SaleRequest(
            "idemp-cash-" + UUID.randomUUID(),
            List.of(), // En este test nos enfocamos en el pago, no en los items
            "CASH",
            "20-12345678-9",
            null,
            null,
            null,
            null
        );

        // When
        SaleCompletedResponse response = salesService.create(request, "tiago_test");

        // Then
        assertThat(response.paymentMethod()).isEqualTo("CASH");
    }

    @Test
    void shouldProcessCreditCardPaymentSuccessfully() {
        stubIdempotencyMissAndSave();

        // Given
        SalesService.SaleRequest request = new SalesService.SaleRequest(
            "idemp-card-" + UUID.randomUUID(),
            List.of(),
            "CREDIT_CARD",
            "20-12345678-9",
            null,
            null,
            null,
            null
        );

        // When
        SaleCompletedResponse response = salesService.create(request, "tiago_test");

        // Then
        assertThat(response.paymentMethod()).isEqualTo("CREDIT_CARD");
    }
}
