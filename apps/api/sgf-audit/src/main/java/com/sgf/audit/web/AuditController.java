package com.sgf.audit.web;

import com.sgf.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit/events")
@Tag(name = "Audit", description = "Endpoints for immutable audit log retrieval")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Get latest audit events", description = "Retrieves the most recent system events with cryptographic integrity checks.")
    public List<AuditEventResponse> latest(
            @Parameter(description = "Maximum number of events to return (max 200)")
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return auditService.latest(Math.min(limit, 200));
    }

    @GetMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Verify audit chain integrity",
            description = "Recomputes the audit hash-chain and reports the first broken link if tampering is detected.")
    public AuditService.AuditChainVerification verify(
            @Parameter(description = "Maximum number of events to verify (max 5000)")
            @RequestParam(name = "limit", defaultValue = "1000") int limit) {
        return auditService.verifyChain(Math.min(limit, 5000));
    }
}
