package com.sgf.pos.web;

import org.springframework.http.ResponseEntity;


import com.sgf.pos.service.SalesService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
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
                               java.security.Principal principal) {
        return salesService.createLegacy(request, principal.getName());
    }
}
