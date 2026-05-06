package com.sgf.modules.catalog.web;

import com.sgf.modules.auth.service.SgfUserPrincipal;
import com.sgf.modules.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request,
                                  @AuthenticationPrincipal SgfUserPrincipal principal) {
        return productService.create(request, principal.getUsername());
    }
}

