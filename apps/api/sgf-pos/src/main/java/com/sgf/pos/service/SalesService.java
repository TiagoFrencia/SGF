package com.sgf.pos.service;

import com.sgf.audit.service.AuditService;
import com.sgf.modules.auth.domain.UserAccount;
import com.sgf.modules.auth.domain.UserAccountRepository;
import com.sgf.core.domain.NotFoundException;
import com.sgf.inventory.service.InventoryService;
import com.sgf.integrations.service.OutboxService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService {

    private final SaleRepository saleRepository;
    private final UserAccountRepository userAccountRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final OutboxService outboxService;

    public SalesService(SaleRepository saleRepository,
                        UserAccountRepository userAccountRepository,
                        InventoryService inventoryService,
                        AuditService auditService,
                        OutboxService outboxService) {
        this.saleRepository = saleRepository;
        this.userAccountRepository = userAccountRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.outboxService = outboxService;
    }

    /**
     * Create sale from POS order flow (new DTO).
     */
    @Transactional
    public SaleCompletedResponse create(SaleRequest request, String actorUsername) {
        UserAccount actor = userAccountRepository.findByUsernameAndActiveTrue(actorUsername)
                .orElseThrow(() -> new NotFoundException("User not found"));

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
        sale.setCreatedBy(actor);
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

        auditService.record(actorUsername, "SALE_COMPLETED", "SALE", saved.getId(),
                "{\"idempotencyKey\":\"" + saved.getExternalIdempotencyKey() + "\",\"total\":" + total + "}");
        outboxService.enqueue("SALE", saved.getId(), "SALE_COMPLETED",
                "{\"idempotencyKey\":\"" + saved.getExternalIdempotencyKey() + "\"}");

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
                                .toList()),
                actorUsername);
        return SaleResponse.fromLegacy(result);
    }

    public record SaleRequest(String idempotencyKey, java.util.List<SaleItemRequest> items,
                              String paymentMethod, String customerDocument) {
    }

    public record SaleItemRequest(UUID productId, int quantity, BigDecimal unitPrice) {
    }
}