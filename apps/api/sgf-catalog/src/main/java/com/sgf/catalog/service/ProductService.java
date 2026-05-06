package com.sgf.catalog.service;

import com.sgf.audit.service.AuditService;
import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductPresentation;
import com.sgf.catalog.domain.ProductRepository;
import java.util.Optional;
import com.sgf.catalog.web.CreateProductRequest;
import com.sgf.catalog.web.ProductResponse;
import com.sgf.core.domain.ConflictException;
import com.sgf.core.domain.NotFoundException;
import com.sgf.integrations.service.OutboxService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditService auditService;
    private final OutboxService outboxService;

    public ProductService(ProductRepository productRepository, AuditService auditService, OutboxService outboxService) {
        this.productRepository = productRepository;
        this.auditService = auditService;
        this.outboxService = outboxService;
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
        auditService.record(actor, "PRODUCT_CREATED", "PRODUCT", saved.getId(), "{\"sku\":\"" + saved.getSku() + "\"}");
        outboxService.enqueue("PRODUCT", saved.getId(), "PRODUCT_CREATED", "{\"sku\":\"" + saved.getSku() + "\"}");
        return ProductResponse.from(saved);
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
        return ProductResponse.from(productRepository.save(product));
    }
}
