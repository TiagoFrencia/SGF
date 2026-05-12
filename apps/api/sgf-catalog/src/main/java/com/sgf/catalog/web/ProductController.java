package com.sgf.catalog.web;


import com.sgf.catalog.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public List<ProductResponse> list(@RequestParam(name = "gtin", required = false) String gtin,
                                      @RequestParam(name = "name", required = false) String name) {
        return productService.list(gtin, name);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public ProductResponse get(@PathVariable("productId") UUID productId) {
        return productService.get(productId);
    }

    @GetMapping("/search/gtin/{gtin}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public ProductResponse getByGtin(@PathVariable("gtin") String gtin) {
        return productService.findResponseByGtin(gtin)
                .orElseThrow(() -> new com.sgf.core.domain.NotFoundException("Product not found for GTIN: " + gtin));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request,
                                  java.security.Principal principal) {
        return productService.create(request, principal.getName());
    }
}
