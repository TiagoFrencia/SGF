package com.sgf.integrations.anmat.web;

import com.sgf.modules.auth.service.SgfUserPrincipal;
import com.sgf.integrations.anmat.domain.AnmatRemediationStatus;
import com.sgf.integrations.anmat.service.AnmatHealthResponse;
import com.sgf.integrations.anmat.service.AnmatTraceabilityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/anmat")
public class AnmatController {

    private final AnmatTraceabilityService traceabilityService;

    public AnmatController(AnmatTraceabilityService traceabilityService) {
        this.traceabilityService = traceabilityService;
    }

    @PostMapping("/datamatrix/parse")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER')")
    public AnmatDataMatrixParseResponse parse(@Valid @RequestBody AnmatDataMatrixParseRequest request) {
        return traceabilityService.parse(request.dataMatrix());
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AnmatHealthResponse health() {
        return traceabilityService.health();
    }

    @PostMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public AnmatTraceabilityEventResponse report(@Valid @RequestBody AnmatTraceabilityEventRequest request,
                                                 @AuthenticationPrincipal SgfUserPrincipal principal) {
        return traceabilityService.report(request, principal.getUsername());
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public List<AnmatTraceabilityEventResponse> latest() {
        return traceabilityService.latest();
    }

    @GetMapping("/events/by-gtin")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public List<AnmatTraceabilityEventResponse> byGtin(@RequestParam String gtin) {
        return traceabilityService.findByGtin(gtin);
    }

    @GetMapping("/events/by-lot")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public List<AnmatTraceabilityEventResponse> byLot(@RequestParam String gtin,
                                                      @RequestParam String lotNumber) {
        return traceabilityService.findByGtinAndLot(gtin, lotNumber);
    }

    @GetMapping("/serial-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AnmatTraceabilitySerialSummaryResponse serialSummary(@RequestParam String gtin,
                                                                @RequestParam String serialNumber) {
        return traceabilityService.serialSummary(gtin, serialNumber);
    }

    @GetMapping("/inconsistencies")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public List<AnmatTraceabilityInconsistencyResponse> inconsistencies() {
        return traceabilityService.inconsistencies();
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AnmatTraceabilityDashboardResponse dashboard() {
        return traceabilityService.dashboard();
    }

    @GetMapping("/remediation-cases")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AnmatRemediationCasePageResponse remediationCases(@RequestParam(required = false) AnmatRemediationStatus status,
                                                             @RequestParam(required = false) String assignedTo,
                                                             @RequestParam(required = false) String severity,
                                                             @RequestParam(required = false) String issueCode,
                                                             @RequestParam(required = false) String gtin,
                                                             @RequestParam(required = false) String serialNumber,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size,
                                                             @RequestParam(defaultValue = "updatedAt") String sortBy,
                                                             @RequestParam(defaultValue = "DESC") String sortDirection) {
        return traceabilityService.remediationCases(
                status,
                assignedTo,
                severity,
                issueCode,
                gtin,
                serialNumber,
                page,
                size,
                sortBy,
                sortDirection
        );
    }

    @PostMapping("/remediation-cases/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AnmatRemediationSyncResponse syncRemediationCases(@AuthenticationPrincipal SgfUserPrincipal principal) {
        return traceabilityService.syncRemediationCases(principal.getUsername());
    }

    @PatchMapping("/remediation-cases/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AnmatRemediationCaseResponse updateRemediationCase(@PathVariable UUID caseId,
                                                              @Valid @RequestBody AnmatRemediationActionRequest request,
                                                              @AuthenticationPrincipal SgfUserPrincipal principal) {
        return traceabilityService.updateRemediationCase(caseId, request, principal.getUsername());
    }
}
