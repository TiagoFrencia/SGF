package com.sgf.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
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

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    SaleRepository saleRepository;

    @Mock
    InventoryService inventoryService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    SalesService service;

    @Test
    void createSaleCalculatesTotal() {
        UUID productId = UUID.randomUUID();
        SalesService.SaleItemRequest item = new SalesService.SaleItemRequest(productId, 2, new BigDecimal("100.00"));
        SalesService.SaleRequest request = new SalesService.SaleRequest("KEY1", List.of(item), "CASH", "123");

        when(saleRepository.findByExternalIdempotencyKey("KEY1")).thenReturn(Optional.empty());
        Batch batch = new Batch();
        batch.setProduct(new Product());
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
}
