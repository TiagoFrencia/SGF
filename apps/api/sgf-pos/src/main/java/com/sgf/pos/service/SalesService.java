package com.sgf.pos.service;

import com.sgf.catalog.service.ProductService;
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
import com.sgf.integrations.pami.dto.SiafarItem;
import com.sgf.integrations.pami.dto.SiafarMessageHeader;
import com.sgf.integrations.pami.dto.SiafarPrescriptionHeader;
import com.sgf.integrations.pami.dto.SiafarValidationRequest;
import com.sgf.integrations.pami.dto.SiafarValidationResponse;
import com.sgf.integrations.pami.service.PamiSiafarService;
import com.sgf.integrations.refeps.dto.ProfessionalLicenseResponse;
import com.sgf.integrations.refeps.service.RefepsService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService {

    private final SaleRepository saleRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final PamiSiafarService pamiService;
    private final RefepsService refepsService;
    private final ProductService productService;

    public SalesService(SaleRepository saleRepository,
                        InventoryService inventoryService,
                        ApplicationEventPublisher eventPublisher,
                        PamiSiafarService pamiService,
                        RefepsService refepsService,
                        ProductService productService) {
        this.saleRepository = saleRepository;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
        this.pamiService = pamiService;
        this.refepsService = refepsService;
        this.productService = productService;
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

        // --- PAMI Validation Logic ---
        if (request.pamiPrescriptionId() != null && !request.pamiPrescriptionId().isBlank()) {
            validateWithPami(request);
        }

        // --- REFEPS Professional Validation ---
        if (request.doctorLicense() != null && !request.doctorLicense().isBlank()) {
            validateProfessional(request);
        }

        sale = new Sale();
        sale.setId(UUID.randomUUID());
        sale.setExternalIdempotencyKey(request.idempotencyKey());
        sale.setStatus("COMPLETED");
        sale.setSoldAt(OffsetDateTime.now());
        sale.setCreatedBy(actorUsername);
        sale.setPaymentMethod(request.paymentMethod());
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
            saved.getTenantId(),
            saved.getSoldAt(),
            saved.getPaymentMethod(),
            request.customerDocument(),
            request.pamiPrescriptionId(),
            request.pamiBeneficiaryId(),
            request.doctorLicense(),
            request.doctorRegion(),
            saved.getItems().stream()
                .map(i -> new SaleCompletedEvent.SaleItemInfo(
                        i.getProduct().getId(),
                        i.getProduct().getGtin(),
                        i.getProduct().getTroquel(),
                        i.getBatch() != null ? i.getBatch().getId() : null,
                        i.getBatch() != null ? i.getBatch().getLotNumber() : null,
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getSubtotal(),
                        i.getProduct().isRequiresTraceability()))
                .toList()
        ));

        return SaleCompletedResponse.from(saved);
    }

    private void validateWithPami(SaleRequest request) {
        SiafarValidationRequest pamiRequest = new SiafarValidationRequest(
                SiafarMessageHeader.defaultHeader("PHARMA-123"), // TODO: Get from branch config
                new SiafarPrescriptionHeader(
                        "PAMI",
                        request.pamiBeneficiaryId(),
                        "GENERAL",
                        "MAT-5566", // TODO: Get from doctor data
                        request.pamiPrescriptionId(),
                        java.time.LocalDate.now(),
                        "N"
                ),
                request.items().stream()
                        .map(i -> new SiafarItem(commercialIdentifierForPami(i.productId()), i.quantity(), i.unitPrice()))
                        .toList()
        );

        SiafarValidationResponse pamiResponse = pamiService.validatePrescription(pamiRequest);

        if (!pamiResponse.isApproved()) {
            throw new RuntimeException("Validación PAMI rechazada: " + pamiResponse.responseMessage());
        }
    }

    private void validateProfessional(SaleRequest request) {
        ProfessionalLicenseResponse refepsResponse = refepsService.validateLicense(
                request.doctorLicense(), 
                request.doctorRegion() != null ? request.doctorRegion() : "NAC"
        );

        if (!refepsResponse.isValid()) {
            throw new RuntimeException("Profesional no habilitado en REFEPS: " + refepsResponse.status());
        }
    }

    private String commercialIdentifierForPami(UUID productId) {
        var product = productService.findEntity(productId);
        if (hasText(product.getTroquel())) {
            return product.getTroquel();
        }
        if (hasText(product.getGtin())) {
            return product.getGtin();
        }
        return productId.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
                        null, null,
                        request.pamiPrescriptionId(), request.pamiBeneficiaryId(),
                        request.doctorLicense(), request.doctorRegion()),
                actorUsername);
        return SaleResponse.fromLegacy(result);
    }

    public record SaleRequest(String idempotencyKey, java.util.List<SaleItemRequest> items,
                              String paymentMethod, String customerDocument,
                              String pamiPrescriptionId, String pamiBeneficiaryId,
                              String doctorLicense, String doctorRegion) {
    }

    public record SaleItemRequest(UUID productId, int quantity, BigDecimal unitPrice) {
    }
}
