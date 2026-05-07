package com.sgf.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.catalog.service.ProductService;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import com.sgf.inventory.domain.StockMovementRepository;
import com.sgf.inventory.web.InventoryReceiptRequest;
import com.sgf.inventory.web.InventoryReceiptResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    BatchRepository batchRepository;

    @Mock
    StockMovementRepository stockMovementRepository;

    @Mock
    ProductService productService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    InventoryService service;

    @Test
    void receiveProductCreatesNewBatch() {
        UUID productId = UUID.randomUUID();
        InventoryReceiptRequest request = new InventoryReceiptRequest(productId, "LOT1", LocalDate.now().plusYears(1), 10, new BigDecimal("100.00"));

        when(productService.findEntity(productId)).thenReturn(new Product());
        when(batchRepository.findByProductIdAndLotNumber(productId, "LOT1")).thenReturn(Optional.empty());
        when(batchRepository.save(any())).thenAnswer(i -> {
            Batch b = (Batch) i.getArguments()[0];
            b.setProduct(new Product());
            return b;
        });

        InventoryReceiptResponse result = service.receive(request, "admin");
        assertNotNull(result);
        assertEquals(10, result.availableQuantity());
    }
}
