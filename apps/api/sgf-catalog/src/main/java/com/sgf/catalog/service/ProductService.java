package com.sgf.catalog.service;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductPriceSnapshot;
import com.sgf.catalog.domain.ProductPriceSnapshotRepository;
import com.sgf.catalog.domain.ProductPresentation;
import com.sgf.catalog.domain.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import com.sgf.catalog.web.CreateProductRequest;
import com.sgf.catalog.web.ProductResponse;
import com.sgf.core.domain.ConflictException;
import com.sgf.core.domain.NotFoundException;
import com.sgf.core.event.ProductCreatedEvent;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPriceSnapshotRepository priceSnapshotRepository;
    private final ProductPricingService productPricingService;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository,
                          ProductPriceSnapshotRepository priceSnapshotRepository,
                          ProductPricingService productPricingService,
                          ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.productPricingService = productPricingService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request, String actor) {
        if (productRepository.existsByGtinOrSku(request.gtin(), request.sku())) {
            throw new ConflictException("Product with same GTIN or SKU already exists");
        }
        Product product = new Product();
        product.setGtin(request.gtin());
        product.setSku(request.sku());
        product.setCommercialName(request.commercialName());
        product.setBrand(request.brand());
        product.setActiveIngredient(request.activeIngredient());
        product.setLaboratory(request.brand());
        product.setPrescriptionRequired(request.prescriptionRequired());
        product.setRequiresTraceability(request.requiresTraceability());
        product.setAnmatCategory(request.anmatCategory());

        ProductPresentation presentation = new ProductPresentation();
        presentation.setProduct(product);
        presentation.setDescription(request.presentationDescription());
        presentation.setConcentration(request.concentration());
        presentation.setForm(request.form());
        presentation.setUnitsPerPackage(request.unitsPerPackage());
        product.setPresentations(new ArrayList<>(List.of(presentation)));

        Product saved = productRepository.save(product);
        
        eventPublisher.publishEvent(new ProductCreatedEvent(
            saved.getId(),
            saved.getGtin(),
            saved.getCommercialName(),
            actor,
            saved.getTenantId(),
            OffsetDateTime.now()
        ));

        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public java.util.List<ProductResponse> list(String gtin, String name) {
        if (gtin != null && !gtin.isBlank()) {
            return productRepository.findByGtin(gtin)
                    .map(this::toResponse)
                    .stream()
                    .toList();
        }
        if (name != null && !name.isBlank()) {
            return searchByName(name, 50).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID productId) {
        return toResponse(findEntity(productId));
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> findResponseByGtin(String gtin) {
        return productRepository.findByGtin(gtin).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Product findEntity(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Transactional(readOnly = true)
    public Product findByGtin(String gtin) {
        return productRepository.findByGtin(gtin)
                .orElseThrow(() -> new NotFoundException("Product not found for GTIN: " + gtin));
    }

    @Transactional(readOnly = true)
    public Optional<Product> findByGtinOptional(String gtin) {
        return productRepository.findByGtin(gtin);
    }

    @Transactional(readOnly = true)
    public List<Product> searchByName(String name, int limit) {
        return productRepository.findByCommercialNameContainingIgnoreCase(name)
                .stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public List<Product> findByActiveIngredient(String activeIngredient) {
        return productRepository.findByActiveIngredientIgnoreCase(activeIngredient);
    }

    @Transactional
    public ProductResponse updateCommercialData(String gtin, String alfabetCode, String kairosCode) {
        Product product = findByGtin(gtin);
        product.setAlfabetCode(alfabetCode);
        product.setKairosCode(kairosCode);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse importFromVademecum(VademecumProductImportCommand command) {
        Product product = findExistingVademecumProduct(command).orElseGet(Product::new);
        boolean isNew = product.getId() == null;

        product.setGtin(nonBlankOrFallback(command.gtin(), generatedGtin(command)));
        product.setSku(nonBlankOrFallback(product.getSku(), generatedSku(command)));
        product.setCommercialName(command.commercialName());
        product.setBrand(command.commercialName());
        product.setActiveIngredient(command.activeIngredient());
        product.setLaboratory(command.laboratory());
        product.setLaboratoryCode(command.laboratoryCode());
        product.setSnomedCode(command.snomedCode());
        product.setTroquel(command.troquel());
        product.setBarcode(command.barcode());
        product.setSource(command.source());
        product.setSourceRecordKey(command.sourceRecordKey());
        product.setSourceUpdatedAt(command.effectiveDate());
        product.setPrescriptionRequired(isPrescriptionRequired(command.saleCondition()));
        product.setRequiresTraceability(false);
        product.setAnmatCategory(null);

        if (isNew || product.getPresentations().isEmpty()) {
            ProductPresentation presentation = new ProductPresentation();
            presentation.setProduct(product);
            presentation.setDescription(nonBlankOrFallback(command.presentation(), "Presentacion no informada"));
            presentation.setForm(command.pharmaceuticalForm());
            presentation.setUnitsPerPackage(command.unitsPerPackage() != null && command.unitsPerPackage() > 0
                    ? command.unitsPerPackage()
                    : 1);
            product.setPresentations(new ArrayList<>(List.of(presentation)));
        } else {
            ProductPresentation presentation = product.getPresentations().getFirst();
            presentation.setDescription(nonBlankOrFallback(command.presentation(), presentation.getDescription()));
            presentation.setForm(nonBlankOrFallback(command.pharmaceuticalForm(), presentation.getForm()));
            if (command.unitsPerPackage() != null && command.unitsPerPackage() > 0) {
                presentation.setUnitsPerPackage(command.unitsPerPackage());
            }
        }

        Product saved = productRepository.save(product);
        upsertPriceSnapshot(saved, command);
        return toResponse(saved);
    }
 
    @Transactional
    public void delete(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found");
        }
        productRepository.deleteById(productId);
    }
 
    @Transactional(readOnly = true)
    public long count() {
        return productRepository.count();
    }

    private Optional<Product> findExistingVademecumProduct(VademecumProductImportCommand command) {
        if (hasText(command.gtin())) {
            Optional<Product> byGtin = productRepository.findByGtin(command.gtin());
            if (byGtin.isPresent()) return byGtin;
        }
        if (hasText(command.troquel())) {
            Optional<Product> byTroquel = productRepository.findByTroquel(command.troquel());
            if (byTroquel.isPresent()) return byTroquel;
        }
        if (hasText(command.commercialName()) && hasText(command.presentation()) && hasText(command.laboratory())) {
            return productRepository.findByCommercialNamePresentationAndLaboratory(
                    command.commercialName(), command.presentation(), command.laboratory());
        }
        return Optional.empty();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.from(product, productPricingService.latestPrice(product).orElse(null));
    }

    private void upsertPriceSnapshot(Product product, VademecumProductImportCommand command) {
        if (command.retailPrice() == null || command.effectiveDate() == null) {
            return;
        }
        ProductPriceSnapshot snapshot = priceSnapshotRepository
                .findByProductAndSourceAndSourceRecordKeyAndEffectiveDate(
                        product, command.source(), command.sourceRecordKey(), command.effectiveDate())
                .orElseGet(ProductPriceSnapshot::new);
        snapshot.setProduct(product);
        snapshot.setSource(command.source());
        snapshot.setSourceRecordKey(command.sourceRecordKey());
        snapshot.setEffectiveDate(command.effectiveDate());
        snapshot.setRetailPrice(command.retailPrice());
        snapshot.setPamiAffiliatePrice(command.pamiAffiliatePrice());
        snapshot.setPamiDiscountCode(command.pamiDiscountCode());
        snapshot.setPamiDiscountLabel(command.pamiDiscountLabel());
        priceSnapshotRepository.save(snapshot);
    }

    private boolean isPrescriptionRequired(String saleCondition) {
        return saleCondition != null && saleCondition.toLowerCase().contains("receta");
    }

    private String generatedSku(VademecumProductImportCommand command) {
        return command.source() + "-" + nonBlankOrFallback(command.sourceRecordKey(), command.gtin());
    }

    private String generatedGtin(VademecumProductImportCommand command) {
        String numeric = nonBlankOrFallback(command.barcode(), command.sourceRecordKey()).replaceAll("\\D", "");
        if (numeric.isBlank()) {
            numeric = "0";
        }
        if (numeric.length() > 14) {
            return numeric.substring(numeric.length() - 14);
        }
        return "0".repeat(14 - numeric.length()) + numeric;
    }

    private String nonBlankOrFallback(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
