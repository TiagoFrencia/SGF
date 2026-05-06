package com.sgf.inventory.service;

import com.sgf.audit.service.AuditService;
import com.sgf.catalog.domain.Product;
import com.sgf.catalog.service.ProductService;
import com.sgf.core.domain.ConflictException;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import com.sgf.inventory.domain.StockMovement;
import com.sgf.inventory.domain.StockMovementRepository;
import com.sgf.inventory.domain.StockMovementType;
import com.sgf.inventory.web.InventoryReceiptRequest;
import com.sgf.inventory.web.InventoryReceiptResponse;
import com.sgf.inventory.web.StockViewResponse;
import com.sgf.integrations.service.OutboxService;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final BatchRepository batchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;
    private final AuditService auditService;
    private final OutboxService outboxService;

    public InventoryService(BatchRepository batchRepository,
                            StockMovementRepository stockMovementRepository,
                            ProductService productService,
                            AuditService auditService,
                            OutboxService outboxService) {
        this.batchRepository = batchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productService = productService;
        this.auditService = auditService;
        this.outboxService = outboxService;
    }

    @Transactional
    public InventoryReceiptResponse receive(InventoryReceiptRequest request, String actor) {
        Product product = productService.findEntity(request.productId());
        Batch batch = batchRepository.findByProductIdAndLotNumber(request.productId(), request.lotNumber())
                .orElseGet(Batch::new);
        if (batch.getId() != null && !batch.getExpiresAt().equals(request.expiresAt())) {
            throw new ConflictException("Existing batch lot number cannot change expiry date");
        }
        batch.setProduct(product);
        batch.setLotNumber(request.lotNumber());
        batch.setExpiresAt(request.expiresAt());
        batch.setUnitCost(request.unitCost());
        batch.setAvailableQuantity((batch.getAvailableQuantity() == null ? 0 : batch.getAvailableQuantity()) + request.quantity());
        Batch saved = batchRepository.save(batch);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setBatch(saved);
        movement.setMovementType(StockMovementType.IN);
        movement.setQuantity(request.quantity());
        movement.setReason("RECEIPT");
        movement.setReferenceId(saved.getId());
        movement.setOccurredAt(OffsetDateTime.now());
        stockMovementRepository.save(movement);

        auditService.record(actor, "INVENTORY_RECEIPT_CREATED", "BATCH", saved.getId(), "{\"lot\":\"" + saved.getLotNumber() + "\"}");
        outboxService.enqueue("BATCH", saved.getId(), "INVENTORY_RECEIPT_CREATED", "{\"lot\":\"" + saved.getLotNumber() + "\"}");
        return InventoryReceiptResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<StockViewResponse> stock() {
        return batchRepository.findAll().stream()
                .sorted(Comparator.comparing(Batch::getExpiresAt))
                .map(StockViewResponse::from)
                .toList();
    }

    @Transactional
    public List<BatchAllocation> reserve(UUID productId, int quantityNeeded, UUID referenceId) {
        List<Batch> eligible = batchRepository
                .findByProductIdAndAvailableQuantityGreaterThanAndExpiresAtGreaterThanEqualOrderByExpiresAtAsc(
                        productId,
                        0,
                        java.time.LocalDate.now()
                );
        int available = eligible.stream().mapToInt(Batch::getAvailableQuantity).sum();
        if (available < quantityNeeded) {
            throw new ConflictException("Insufficient stock for product " + productId);
        }

        int remaining = quantityNeeded;
        java.util.ArrayList<BatchAllocation> allocations = new java.util.ArrayList<>();
        for (Batch batch : eligible) {
            if (remaining == 0) {
                break;
            }
            int taken = Math.min(batch.getAvailableQuantity(), remaining);
            batch.setAvailableQuantity(batch.getAvailableQuantity() - taken);
            stockMovementRepository.save(movementFor(batch, batch.getProduct(), taken, referenceId));
            allocations.add(new BatchAllocation(batch, taken));
            remaining -= taken;
        }
        return allocations;
    }

    private StockMovement movementFor(Batch batch, Product product, int quantity, UUID referenceId) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setBatch(batch);
        movement.setMovementType(StockMovementType.OUT);
        movement.setQuantity(quantity);
        movement.setReason("SALE");
        movement.setReferenceId(referenceId);
        movement.setOccurredAt(OffsetDateTime.now());
        return movement;
    }

    public record BatchAllocation(Batch batch, int quantity) {
    }

    @Transactional(readOnly = true)
    public List<MovementSummary> getMovementsForProduct(UUID productId, int windowDays) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(windowDays);
        return stockMovementRepository.findOutMovementsSince(productId, since)
                .stream()
                .map(m -> new MovementSummary(m.getOccurredAt(), m.getQuantity()))
                .toList();
    }

    public record MovementSummary(OffsetDateTime occurredAt, int quantity) {
    }
}
