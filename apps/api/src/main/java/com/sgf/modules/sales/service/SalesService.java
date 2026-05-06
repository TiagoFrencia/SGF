package com.sgf.modules.sales.service;

import com.sgf.modules.audit.service.AuditService;
import com.sgf.modules.auth.domain.UserAccount;
import com.sgf.modules.auth.domain.UserAccountRepository;
import com.sgf.modules.core.NotFoundException;
import com.sgf.modules.inventory.service.InventoryService;
import com.sgf.modules.integrations.service.OutboxService;
import com.sgf.modules.sales.domain.Sale;
import com.sgf.modules.sales.domain.SaleItem;
import com.sgf.modules.sales.domain.SaleRepository;
import com.sgf.modules.sales.web.CreateSaleRequest;
import com.sgf.modules.sales.web.SaleResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

    @Transactional
    public SaleResponse create(CreateSaleRequest request, String actorUsername) {
        return saleRepository.findByExternalIdempotencyKey(request.idempotencyKey())
                .map(SaleResponse::from)
                .orElseGet(() -> createNew(request, actorUsername));
    }

    private SaleResponse createNew(CreateSaleRequest request, String actorUsername) {
        UserAccount actor = userAccountRepository.findByUsernameAndActiveTrue(actorUsername)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Sale sale = new Sale();
        sale.setId(java.util.UUID.randomUUID());
        sale.setExternalIdempotencyKey(request.idempotencyKey());
        sale.setStatus("COMPLETED");
        sale.setSoldAt(OffsetDateTime.now());
        sale.setCreatedBy(actor);
        sale.setItems(new ArrayList<>());

        BigDecimal total = BigDecimal.ZERO;
        for (CreateSaleRequest.SaleLineRequest line : request.items()) {
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
        auditService.record(actorUsername, "SALE_COMPLETED", "SALE", saved.getId(), "{\"idempotencyKey\":\"" + saved.getExternalIdempotencyKey() + "\"}");
        outboxService.enqueue("SALE", saved.getId(), "SALE_COMPLETED", "{\"idempotencyKey\":\"" + saved.getExternalIdempotencyKey() + "\"}");
        return SaleResponse.from(saved);
    }
}
