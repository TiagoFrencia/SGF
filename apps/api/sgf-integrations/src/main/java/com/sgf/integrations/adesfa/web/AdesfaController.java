package com.sgf.integrations.adesfa.web;


import com.sgf.integrations.adesfa.service.AdesfaHealthResponse;
import com.sgf.integrations.adesfa.service.AdesfaService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/adesfa")
public class AdesfaController {

    private final AdesfaService adesfaService;

    public AdesfaController(AdesfaService adesfaService) {
        this.adesfaService = adesfaService;
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AdesfaHealthResponse health() {
        return adesfaService.health();
    }

    @PostMapping("/validations/sales/{saleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public AdesfaValidationResponse validateSale(@PathVariable UUID saleId,
                                                 @Valid @RequestBody AdesfaValidationRequest request,
                                                 java.security.Principal principal) {
        return adesfaService.validateSale(saleId, request, principal.getName());
    }

    @GetMapping("/validations/{validationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AdesfaValidationResponse getValidation(@PathVariable UUID validationId) {
        return adesfaService.getValidation(validationId);
    }

    @GetMapping("/validations")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public List<AdesfaValidationResponse> latest() {
        return adesfaService.latest();
    }
}
