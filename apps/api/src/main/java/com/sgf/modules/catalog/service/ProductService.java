package com.sgf.modules.catalog.service;

import com.sgf.modules.audit.service.AuditService;
import com.sgf.modules.catalog.domain.Product;
import com.sgf.modules.catalog.domain.ProductPresentation;
import com.sgf.modules.catalog.domain.ProductRepository;
import com.sgf.modules.catalog.web.CreateProductRequest;
import com.sgf.modules.catalog.web.ProductResponse;
import com.sgf.modules.core.ConflictException;
import com.sgf.modules.core.NotFoundException;
import com.sgf.modules.integrations.service.OutboxService;
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
}
