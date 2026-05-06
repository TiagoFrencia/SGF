package com.sgf.modules.sales.web;

import com.sgf.modules.auth.service.SgfUserPrincipal;
import com.sgf.modules.sales.service.SalesService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public SaleResponse create(@Valid @RequestBody CreateSaleRequest request,
                               @AuthenticationPrincipal SgfUserPrincipal principal) {
        return salesService.create(request, principal.getUsername());
    }
}

