package com.sgf.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.service.ProductPriceQuote;
import com.sgf.catalog.service.ProductPricingService;
import com.sgf.catalog.service.ProductService;
import com.sgf.core.domain.BadRequestException;
import com.sgf.inventory.service.InventoryService;
import com.sgf.pos.domain.PosOrder;
import com.sgf.pos.domain.PosOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosOrderServiceTest {

    @Mock PosOrderRepository orderRepository;
    @Mock ProductService productService;
    @Mock ProductPricingService productPricingService;
    @Mock InventoryService inventoryService;
    @Mock SalesService salesService;

    @Test
    void scanAddUsesLatestCatalogPriceWhenManualPriceIsMissing() {
        UUID orderId = UUID.randomUUID();
        Product product = product("07798006301810");
        PosOrder order = draftOrder();
        PosOrderService service = service();

        when(orderRepository.findByIdAndStatus(orderId, PosOrder.OrderStatus.DRAFT)).thenReturn(Optional.of(order));
        when(productService.findByGtin("07798006301810")).thenReturn(product);
        when(productService.findEntity(product.getId())).thenReturn(product);
        when(productPricingService.latestPrice(product)).thenReturn(Optional.of(new ProductPriceQuote(
                new BigDecimal("615.00"), new BigDecimal("369.00"), 7,
                "40% de descuento", "CNPM_MSAL", LocalDate.of(2026, 5, 11))));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PosOrder result = service.scanAdd(orderId, "07798006301810", 1, null);

        assertEquals(new BigDecimal("615.00"), result.getItems().getFirst().getUnitPrice());
    }

    @Test
    void scanAddRequiresManualPriceWhenCatalogPriceIsMissing() {
        UUID orderId = UUID.randomUUID();
        Product product = product("07798006301810");
        PosOrderService service = service();

        when(orderRepository.findByIdAndStatus(orderId, PosOrder.OrderStatus.DRAFT)).thenReturn(Optional.of(draftOrder()));
        when(productService.findByGtin("07798006301810")).thenReturn(product);
        when(productService.findEntity(product.getId())).thenReturn(product);
        when(productPricingService.latestPrice(product)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.scanAdd(orderId, "07798006301810", 1, null));
    }

    @Test
    void scanAddKeepsManualPriceOverride() {
        UUID orderId = UUID.randomUUID();
        Product product = product("07798006301810");
        PosOrder order = draftOrder();
        PosOrderService service = service();

        when(orderRepository.findByIdAndStatus(orderId, PosOrder.OrderStatus.DRAFT)).thenReturn(Optional.of(order));
        when(productService.findByGtin("07798006301810")).thenReturn(product);
        when(productService.findEntity(product.getId())).thenReturn(product);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PosOrder result = service.scanAdd(orderId, "07798006301810", 1, new BigDecimal("700.00"));

        assertEquals(new BigDecimal("700.00"), result.getItems().getFirst().getUnitPrice());
    }

    private PosOrderService service() {
        return new PosOrderService(orderRepository, productService, productPricingService, inventoryService, salesService);
    }

    private Product product(String gtin) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setGtin(gtin);
        product.setCommercialName("IBUPROFENO FECOFAR");
        product.setTroquel("554742");
        return product;
    }

    private PosOrder draftOrder() {
        PosOrder order = new PosOrder();
        order.setBranchId(UUID.randomUUID());
        order.setOrderNumber(1);
        return order;
    }
}
