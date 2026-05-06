package com.sgf.integrations.afip.web;

import com.sgf.modules.auth.service.SgfUserPrincipal;
import com.sgf.integrations.afip.service.AfipConnectivityService;
import com.sgf.integrations.afip.service.AfipService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/afip/invoices")
public class AfipController {

    private final AfipService afipService;
    private final AfipConnectivityService afipConnectivityService;

    public AfipController(AfipService afipService, AfipConnectivityService afipConnectivityService) {
        this.afipService = afipService;
        this.afipConnectivityService = afipConnectivityService;
    }

    @PostMapping("/sales/{saleId}/authorize")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public AfipInvoiceResponse authorize(@PathVariable UUID saleId,
                                         @Valid @RequestBody AfipAuthorizeInvoiceRequest request,
                                         @AuthenticationPrincipal SgfUserPrincipal principal) {
        return afipService.authorizeSaleInvoice(saleId, request, principal.getUsername());
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AfipInvoiceResponse getById(@PathVariable UUID invoiceId) {
        return afipService.getInvoice(invoiceId);
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AfipHealthResponse health(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean refreshToken) {
        return afipConnectivityService.inspect(refreshToken);
    }
}
