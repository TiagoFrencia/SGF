package com.sgf.pos.service;

import com.sgf.core.domain.NotFoundException;
import com.sgf.core.event.SaleCompletedEvent;
import com.sgf.inventory.service.InventoryService;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleItem;
import com.sgf.pos.domain.SaleRepository;
import com.sgf.pos.web.CreateSaleRequest;
import com.sgf.pos.web.SaleCompletedResponse;
import com.sgf.pos.web.SaleResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService {

    private final SaleRepository saleRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    public SalesService(SaleRepository saleRepository,
                        InventoryService inventoryService,
                        ApplicationEventPublisher eventPublisher) {
        this.saleRepository = saleRepository;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create sale from POS order flow (new DTO).
     */
    @Transactional
    public SaleCompletedResponse create(SaleRequest request, String actorUsername) {

        Sale sale = saleRepository.findByExternalIdempotencyKey(request.idempotencyKey())
                .orElse(null);

        if (sale != null) {
            return SaleCompletedResponse.from(sale);
        }

        sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setExternalIdempotencyKey(request.idempotencyKey());
        sale.setStatus("COMPLETED");
        sale.setSoldAt(OffsetDateTime.now());
        sale.setCreatedBy(actorUsername);
        sale.setItems(new ArrayList<>());

        BigDecimal total = BigDecimal.ZERO;
        for (SaleItemRequest line : request.items()) {
            var allocations = inventoryService.reserve(line.productId(), line.quantity(), sale.getId());
            for (var allocation : allocations) {
                SaleItem item = new SaleItem();
                item.setSale(sale);
                item.setProduct(allocation.batch().getProduct());
                item.setBatch(allocation.batch());
                item.setQuantity(allocation.quantity());
                item.setUnitPrice(line.unitPrice());
                item.setSubtotal(line.unitPrice().multiply(BigDecimal.valueOf(allocation.quantity())));
                sale.getItems().add(item);
                total = total.add(item.getSubtotal());
            }
        }
        sale.setTotalAmount(total);
        Sale saved = saleRepository.save(sale);

        eventPublisher.publishEvent(new SaleCompletedEvent(
            saved.getId(),
            saved.getExternalIdempotencyKey(),
            saved.getTotalAmount(),
            actorUsername,
            saved.getSoldAt()
        ));

        return SaleCompletedResponse.from(saved);
    }

    /**
     * Legacy DTO path (used by controllers that haven't migrated yet).
     */
    @Transactional
    public SaleResponse createLegacy(CreateSaleRequest request, String actorUsername) {
        SaleCompletedResponse result = create(
                new SaleRequest(request.idempotencyKey(),
                        request.items().stream()
                                .map(i -> new SaleItemRequest(i.productId(), i.quantity(), i.unitPrice()))
                                .toList(),
                        null, null),
                actorUsername);
        return SaleResponse.fromLegacy(result);
    }

    public record SaleRequest(String idempotencyKey, java.util.List<SaleItemRequest> items,
                              String paymentMethod, String customerDocument) {
    }

    public record SaleItemRequest(UUID productId, int quantity, BigDecimal unitPrice) {
    }
}