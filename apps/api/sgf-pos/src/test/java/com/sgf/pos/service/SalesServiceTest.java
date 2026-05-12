package com.sgf.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgf.integrations.pami.service.PamiSiafarService;
import com.sgf.integrations.pami.dto.SiafarValidationRequest;
import com.sgf.integrations.pami.dto.SiafarValidationResponse;
import com.sgf.integrations.refeps.service.RefepsService;
import com.sgf.catalog.domain.Product;
import com.sgf.catalog.service.ProductService;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.service.InventoryService;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleRepository;
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
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    SaleRepository saleRepository;

    @Mock
    InventoryService inventoryService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    PamiSiafarService pamiService;

    @Mock
    RefepsService refepsService;

    @Mock
    ProductService productService;

    @InjectMocks
    SalesService service;

    @Test
    void createSaleCalculatesTotal() {
        UUID productId = UUID.randomUUID();
        SalesService.SaleItemRequest item = new SalesService.SaleItemRequest(productId, 2, new BigDecimal("100.00"));
        SalesService.SaleRequest request = new SalesService.SaleRequest("KEY1", List.of(item), "CASH", "123", null, null, null, null);

        when(saleRepository.findByExternalIdempotencyKey("KEY1")).thenReturn(Optional.empty());
        Batch batch = new Batch();
        Product product = new Product();
        product.setId(productId);
        product.setGtin("07798006301810");
        product.setTroquel("554742");
        batch.setProduct(product);
        when(inventoryService.reserve(any(), any(Integer.class), any())).thenReturn(List.of(new InventoryService.BatchAllocation(batch, 2)));
        when(saleRepository.save(any())).thenAnswer(i -> {
            Sale s = (Sale) i.getArguments()[0];
            s.setId(UUID.randomUUID());
            return s;
        });

        SaleCompletedResponse result = service.create(request, "admin");
        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.totalAmount());
    }

    @Test
    void pamiValidationUsesTroquelBeforeGtinOrProductId() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setGtin("07798006301810");
        product.setTroquel("554742");
        Batch batch = new Batch();
        batch.setProduct(product);

        SalesService.SaleItemRequest item = new SalesService.SaleItemRequest(productId, 1, new BigDecimal("615.00"));
        SalesService.SaleRequest request = new SalesService.SaleRequest(
                "KEY-PAMI", List.of(item), "CASH", "123",
                "RX-1", "BEN-1", null, null);

        when(saleRepository.findByExternalIdempotencyKey("KEY-PAMI")).thenReturn(Optional.empty());
        when(productService.findEntity(productId)).thenReturn(product);
        when(pamiService.validatePrescription(any())).thenReturn(new SiafarValidationResponse("0", "OK", "AUTH-1", List.of()));
        when(inventoryService.reserve(any(), any(Integer.class), any())).thenReturn(List.of(new InventoryService.BatchAllocation(batch, 1)));
        when(saleRepository.save(any())).thenAnswer(i -> {
            Sale s = (Sale) i.getArguments()[0];
            s.setId(UUID.randomUUID());
            return s;
        });

        service.create(request, "admin");

        ArgumentCaptor<SiafarValidationRequest> captor = ArgumentCaptor.forClass(SiafarValidationRequest.class);
        verify(pamiService).validatePrescription(captor.capture());
        assertEquals("554742", captor.getValue().items().getFirst().troquelCode());
    }
}
