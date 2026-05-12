package com.sgf.integrations.anmat.web;


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
                                                 java.security.Principal principal) {
        return traceabilityService.report(request, principal.getName());
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public List<AnmatTraceabilityEventResponse> latest() {
        return traceabilityService.latest();
    }

    @GetMapping("/events/by-gtin")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public List<AnmatTraceabilityEventResponse> byGtin(@RequestParam("gtin") String gtin) {
        return traceabilityService.findByGtin(gtin);
    }

    @GetMapping("/events/by-lot")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public List<AnmatTraceabilityEventResponse> byLot(@RequestParam("gtin") String gtin,
                                                      @RequestParam("lotNumber") String lotNumber) {
        return traceabilityService.findByGtinAndLot(gtin, lotNumber);
    }

    @GetMapping("/serial-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AnmatTraceabilitySerialSummaryResponse serialSummary(@RequestParam("gtin") String gtin,
                                                                @RequestParam("serialNumber") String serialNumber) {
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
    public AnmatRemediationCasePageResponse remediationCases(@RequestParam(name = "status", required = false) AnmatRemediationStatus status,
                                                             @RequestParam(name = "assignedTo", required = false) String assignedTo,
                                                             @RequestParam(name = "severity", required = false) String severity,
                                                             @RequestParam(name = "issueCode", required = false) String issueCode,
                                                             @RequestParam(name = "gtin", required = false) String gtin,
                                                             @RequestParam(name = "serialNumber", required = false) String serialNumber,
                                                             @RequestParam(name = "page", defaultValue = "0") int page,
                                                             @RequestParam(name = "size", defaultValue = "20") int size,
                                                             @RequestParam(name = "sortBy", defaultValue = "updatedAt") String sortBy,
                                                             @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection) {
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
    public AnmatRemediationSyncResponse syncRemediationCases(java.security.Principal principal) {
        return traceabilityService.syncRemediationCases(principal.getName());
    }

    @PatchMapping("/remediation-cases/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'AUDITOR')")
    public AnmatRemediationCaseResponse updateRemediationCase(@PathVariable("caseId") UUID caseId,
                                                              @Valid @RequestBody AnmatRemediationActionRequest request,
                                                              java.security.Principal principal) {
        return traceabilityService.updateRemediationCase(caseId, request, principal.getName());
    }
}
