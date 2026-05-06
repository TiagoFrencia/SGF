package com.sgf.modules.integrations.anmat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.modules.audit.service.AuditService;
import com.sgf.modules.catalog.domain.Product;
import com.sgf.modules.catalog.domain.ProductRepository;
import com.sgf.modules.core.BadRequestException;
import com.sgf.modules.core.ConflictException;
import com.sgf.modules.core.NotFoundException;
import com.sgf.modules.integrations.anmat.domain.AnmatEventStatus;
import com.sgf.modules.integrations.anmat.domain.AnmatEventType;
import com.sgf.modules.integrations.anmat.domain.AnmatRemediationCase;
import com.sgf.modules.integrations.anmat.domain.AnmatRemediationCaseRepository;
import com.sgf.modules.integrations.anmat.domain.AnmatRemediationStatus;
import com.sgf.modules.integrations.anmat.domain.AnmatTraceabilityEvent;
import com.sgf.modules.integrations.anmat.domain.AnmatTraceabilityEventRepository;
import com.sgf.modules.integrations.anmat.web.AnmatDataMatrixParseResponse;
import com.sgf.modules.integrations.anmat.web.AnmatTraceabilityDashboardResponse;
import com.sgf.modules.integrations.anmat.web.AnmatTraceabilityEventRequest;
import com.sgf.modules.integrations.anmat.web.AnmatTraceabilityEventResponse;
import com.sgf.modules.integrations.anmat.web.AnmatTraceabilityInconsistencyResponse;
import com.sgf.modules.integrations.anmat.web.AnmatRemediationActionRequest;
import com.sgf.modules.integrations.anmat.web.AnmatRemediationCaseResponse;
import com.sgf.modules.integrations.anmat.web.AnmatRemediationCasePageResponse;
import com.sgf.modules.integrations.anmat.web.AnmatRemediationSyncResponse;
import com.sgf.modules.integrations.anmat.web.AnmatTraceabilitySerialSummaryResponse;
import com.sgf.modules.integrations.service.OutboxService;
import com.sgf.modules.inventory.domain.Batch;
import com.sgf.modules.inventory.domain.BatchRepository;
import com.sgf.modules.sales.domain.Sale;
import com.sgf.modules.sales.domain.SaleItem;
import com.sgf.modules.sales.domain.SaleRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnmatTraceabilityService {

    private final AnmatDataMatrixParser parser;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final SaleRepository saleRepository;
    private final AnmatTraceabilityEventRepository eventRepository;
    private final AnmatRemediationCaseRepository remediationCaseRepository;
    private final AnmatTraceabilityGateway gateway;
    private final AnmatProperties anmatProperties;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public AnmatTraceabilityService(AnmatDataMatrixParser parser,
                                    ProductRepository productRepository,
                                    BatchRepository batchRepository,
                                    SaleRepository saleRepository,
                                    AnmatTraceabilityEventRepository eventRepository,
                                    AnmatRemediationCaseRepository remediationCaseRepository,
                                    AnmatTraceabilityGateway gateway,
                                    AnmatProperties anmatProperties,
                                    AuditService auditService,
                                    OutboxService outboxService,
                                    ObjectMapper objectMapper) {
        this.parser = parser;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.saleRepository = saleRepository;
        this.eventRepository = eventRepository;
        this.remediationCaseRepository = remediationCaseRepository;
        this.gateway = gateway;
        this.anmatProperties = anmatProperties;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AnmatDataMatrixParseResponse parse(String rawCode) {
        return AnmatDataMatrixParseResponse.from(parser.parse(rawCode));
    }

    @Transactional(readOnly = true)
    public AnmatHealthResponse health() {
        String status = !anmatProperties.enabled()
                ? "DISABLED"
                : anmatProperties.mode() == AnmatMode.SANDBOX ? "SANDBOX_READY" : "PRODUCTION_CONFIGURED";
        return new AnmatHealthResponse(
                anmatProperties.enabled(),
                anmatProperties.mode().name(),
                anmatProperties.baseUrl(),
                anmatProperties.reportPath(),
                status
        );
    }

    @Transactional
    public AnmatTraceabilityEventResponse report(AnmatTraceabilityEventRequest request, String actorUsername) {
        AnmatDataMatrix parsed = parser.parse(request.dataMatrix());
        Product product = productRepository.findByGtin(parsed.gtin())
                .orElseThrow(() -> new NotFoundException("Product with GTIN %s not found".formatted(parsed.gtin())));
        if (!product.isRequiresTraceability()) {
            throw new BadRequestException("Product is not marked as traceable");
        }
        eventRepository.findByEventTypeAndGtinAndSerialNumber(request.eventType(), parsed.gtin(), parsed.serialNumber())
                .ifPresent(existing -> {
                    throw new ConflictException("Traceability event already exists for this serial and event type");
                });

        Batch batch = batchRepository.findByProductIdAndLotNumber(product.getId(), parsed.lotNumber())
                .orElseThrow(() -> new NotFoundException("Batch %s not found for product".formatted(parsed.lotNumber())));

        Sale sale = null;
        if (request.saleId() != null) {
            sale = saleRepository.findById(request.saleId())
                    .orElseThrow(() -> new NotFoundException("Sale not found"));
        }
        validateEventConsistency(request, product, batch, sale);

        TraceabilityReportCommand command = new TraceabilityReportCommand(
                product.getId(),
                batch.getId(),
                sale != null ? sale.getId() : null,
                request.eventType(),
                parsed.gtin(),
                parsed.serialNumber(),
                parsed.lotNumber(),
                parsed.expiresAt(),
                request.gln(),
                request.occurredAt() != null ? request.occurredAt() : OffsetDateTime.now()
        );

        AnmatTraceabilityGateway.GatewayResult gatewayResult = gateway.report(command);

        AnmatTraceabilityEvent event = new AnmatTraceabilityEvent();
        event.setProduct(product);
        event.setBatch(batch);
        event.setSale(sale);
        event.setEventType(request.eventType());
        event.setEventStatus(gatewayResult.success() ? AnmatEventStatus.REPORTED : AnmatEventStatus.FAILED);
        event.setGtin(parsed.gtin());
        event.setSerialNumber(parsed.serialNumber());
        event.setLotNumber(parsed.lotNumber());
        event.setExpiresAt(parsed.expiresAt());
        event.setGln(request.gln());
        event.setOccurredAt(command.occurredAt());
        event.setSource(request.source());
        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("eventType", request.eventType());
        requestPayload.put("dataMatrix", request.dataMatrix());
        requestPayload.put("gln", request.gln());
        requestPayload.put("saleId", request.saleId());
        requestPayload.put("source", request.source());
        event.setRequestJson(toJson(requestPayload));
        event.setResponseJson(gatewayResult.payload());
        event.setErrorMessage(gatewayResult.errorMessage());
        event.setProviderReference(gatewayResult.providerReference());
        event.setIntegrationMode(gatewayResult.integrationMode());
        event.setLastHttpStatus(gatewayResult.httpStatus());
        event.setRetryable(gatewayResult.retryable());
        AnmatTraceabilityEvent saved = eventRepository.save(event);

        auditService.record(actorUsername, "ANMAT_TRACEABILITY_REPORTED", "ANMAT_EVENT", saved.getId(),
                "{\"eventType\":\"" + saved.getEventType() + "\",\"serial\":\"" + saved.getSerialNumber() + "\"}");
        outboxService.enqueue("ANMAT_EVENT", saved.getId(), "ANMAT_TRACEABILITY_" + saved.getEventStatus().name(),
                "{\"eventType\":\"" + saved.getEventType() + "\",\"status\":\"" + saved.getEventStatus() + "\"}");
        return AnmatTraceabilityEventResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AnmatTraceabilityEventResponse> latest() {
        return eventRepository.findTop50ByOrderByOccurredAtDesc().stream()
                .map(AnmatTraceabilityEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnmatTraceabilityEventResponse> findByGtin(String gtin) {
        return eventRepository.findTop100ByGtinOrderByOccurredAtDesc(gtin).stream()
                .map(AnmatTraceabilityEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnmatTraceabilityEventResponse> findByGtinAndLot(String gtin, String lotNumber) {
        return eventRepository.findTop100ByGtinAndLotNumberOrderByOccurredAtDesc(gtin, lotNumber).stream()
                .map(AnmatTraceabilityEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnmatTraceabilitySerialSummaryResponse serialSummary(String gtin, String serialNumber) {
        List<AnmatTraceabilityEvent> timeline = eventRepository.findByGtinAndSerialNumberOrderByOccurredAtAsc(gtin, serialNumber);
        if (timeline.isEmpty()) {
            throw new NotFoundException("No traceability events found for serial");
        }
        boolean hasReceipt = timeline.stream().anyMatch(event -> event.getEventType() == AnmatEventType.RECEIPT);
        boolean hasDispense = timeline.stream().anyMatch(event -> event.getEventType() == AnmatEventType.DISPENSE);
        boolean hasReturn = timeline.stream().anyMatch(event -> event.getEventType() == AnmatEventType.RETURN);
        AnmatTraceabilityEvent latest = timeline.getLast();
        return new AnmatTraceabilitySerialSummaryResponse(
                gtin,
                serialNumber,
                latest.getLotNumber(),
                latest.getEventType().name(),
                hasReceipt,
                hasDispense,
                hasReturn,
                timeline.size(),
                timeline.stream().map(AnmatTraceabilityEventResponse::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<AnmatTraceabilityInconsistencyResponse> inconsistencies() {
        return buildInconsistencies(eventRepository.findAll());
    }

    @Transactional(readOnly = true)
    public AnmatTraceabilityDashboardResponse dashboard() {
        List<AnmatTraceabilityEvent> all = eventRepository.findAll();
        List<AnmatTraceabilityInconsistencyResponse> inconsistencies = buildInconsistencies(all);
        Map<String, List<AnmatTraceabilityEvent>> bySerial = groupBySerial(all);

        long totalEvents = all.size();
        long receipts = all.stream().filter(event -> event.getEventType() == AnmatEventType.RECEIPT).count();
        long dispenses = all.stream().filter(event -> event.getEventType() == AnmatEventType.DISPENSE).count();
        long returns = all.stream().filter(event -> event.getEventType() == AnmatEventType.RETURN).count();
        long failed = all.stream().filter(event -> event.getEventStatus() == AnmatEventStatus.FAILED).count();
        long currentDispensed = bySerial.values().stream()
                .map(list -> list.stream().sorted(java.util.Comparator.comparing(AnmatTraceabilityEvent::getOccurredAt)).toList())
                .filter(list -> !list.isEmpty() && list.getLast().getEventType() == AnmatEventType.DISPENSE)
                .count();

        return new AnmatTraceabilityDashboardResponse(
                totalEvents,
                receipts,
                dispenses,
                returns,
                failed,
                currentDispensed,
                inconsistencies.size(),
                inconsistencies.stream().limit(20).toList()
        );
    }

    @Transactional(readOnly = true)
    public AnmatRemediationCasePageResponse remediationCases(AnmatRemediationStatus status,
                                                             String assignedTo,
                                                             String severity,
                                                             String issueCode,
                                                             String gtin,
                                                             String serialNumber,
                                                             int page,
                                                             int size,
                                                             String sortBy,
                                                             String sortDirection) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        Sort.Direction direction = normalizeSortDirection(sortDirection);
        PageRequest pageRequest = PageRequest.of(page, normalizeSize(size), Sort.by(direction, normalizedSortBy));
        Page<AnmatRemediationCase> result = remediationCaseRepository.findAll(
                remediationCaseSpecification(status, assignedTo, severity, issueCode, gtin, serialNumber),
                pageRequest
        );
        return new AnmatRemediationCasePageResponse(
                result.getContent().stream().map(AnmatRemediationCaseResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                normalizedSortBy,
                direction.name()
        );
    }

    @Transactional
    public AnmatRemediationSyncResponse syncRemediationCases(String actorUsername) {
        List<AnmatTraceabilityInconsistencyResponse> inconsistencies = buildInconsistencies(eventRepository.findAll());
        int created = 0;
        for (AnmatTraceabilityInconsistencyResponse inconsistency : inconsistencies) {
            if (ensureRemediationCase(inconsistency)) {
                created++;
            }
        }
        auditService.record(actorUsername, "ANMAT_REMEDIATION_SYNC", "ANMAT_REMEDIATION", null,
                "{\"inconsistenciesFound\":" + inconsistencies.size() + ",\"created\":" + created + "}");
        return new AnmatRemediationSyncResponse(inconsistencies.size(), created);
    }

    @Transactional
    public AnmatRemediationCaseResponse updateRemediationCase(UUID caseId,
                                                              AnmatRemediationActionRequest request,
                                                              String actorUsername) {
        AnmatRemediationCase remediationCase = remediationCaseRepository.findById(caseId)
                .orElseThrow(() -> new NotFoundException("Remediation case not found"));
        validateRemediationTransition(remediationCase, request);
        remediationCase.setStatus(request.status());
        if (request.notes() != null) {
            remediationCase.setNotes(request.notes());
        }
        if (request.assignedTo() != null) {
            remediationCase.setAssignedTo(request.assignedTo());
        }
        remediationCase.setLastReason(request.reason());
        remediationCase.setLastActionBy(actorUsername);
        remediationCase.setResolvedAt(request.status() == AnmatRemediationStatus.RESOLVED ? OffsetDateTime.now() : null);
        AnmatRemediationCase saved = remediationCaseRepository.saveAndFlush(remediationCase);
        auditService.record(actorUsername, "ANMAT_REMEDIATION_UPDATED", "ANMAT_REMEDIATION", saved.getId(),
                "{\"issueCode\":\"" + saved.getIssueCode() + "\",\"status\":\"" + saved.getStatus() + "\",\"reason\":\"" + safeJson(request.reason()) + "\"}");
        return AnmatRemediationCaseResponse.from(saved);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize ANMAT payload", ex);
        }
    }

    private void validateEventConsistency(AnmatTraceabilityEventRequest request, Product product, Batch batch, Sale sale) {
        AnmatDataMatrix parsed = parser.parse(request.dataMatrix());
        List<AnmatTraceabilityEvent> serialTimeline = eventRepository.findByGtinAndSerialNumberOrderByOccurredAtAsc(parsed.gtin(), parsed.serialNumber());

        if (request.eventType() == AnmatEventType.DISPENSE) {
            if (sale == null) {
                throw new BadRequestException("DISPENSE event requires a saleId");
            }
            boolean sold = sale.getItems().stream().anyMatch(item -> matches(item, product.getId(), batch.getId()));
            if (!sold) {
                throw new BadRequestException("Sale does not contain the traced product/batch for DISPENSE");
            }
            boolean hasReceipt = serialTimeline.stream().anyMatch(event -> event.getEventType() == AnmatEventType.RECEIPT);
            if (!hasReceipt) {
                throw new BadRequestException("DISPENSE event requires a prior RECEIPT for the same serial");
            }
            boolean alreadyDispensed = serialTimeline.stream().anyMatch(event -> event.getEventType() == AnmatEventType.DISPENSE);
            boolean returnedAfterDispense = serialTimeline.stream().anyMatch(event -> event.getEventType() == AnmatEventType.RETURN);
            if (alreadyDispensed && !returnedAfterDispense) {
                throw new ConflictException("Serial is already dispensed and has not been returned");
            }
        }
        if (request.eventType() == AnmatEventType.RECEIPT && sale != null) {
            throw new BadRequestException("RECEIPT event should not reference a sale");
        }
        if (request.eventType() == AnmatEventType.RETURN) {
            boolean hadDispense = serialTimeline.stream().anyMatch(event -> event.getEventType() == AnmatEventType.DISPENSE);
            if (!hadDispense) {
                throw new BadRequestException("RETURN event requires a prior DISPENSE for the same serial");
            }
        }
    }

    private boolean matches(SaleItem item, UUID productId, UUID batchId) {
        return item.getProduct().getId().equals(productId) && item.getBatch().getId().equals(batchId);
    }

    private List<AnmatTraceabilityInconsistencyResponse> buildInconsistencies(List<AnmatTraceabilityEvent> events) {
        return groupBySerial(events).values().stream()
                .map(list -> list.stream().sorted(java.util.Comparator.comparing(AnmatTraceabilityEvent::getOccurredAt)).toList())
                .flatMap(list -> detectInconsistencies(list).stream())
                .toList();
    }

    private List<AnmatTraceabilityInconsistencyResponse> detectInconsistencies(List<AnmatTraceabilityEvent> timeline) {
        if (timeline.isEmpty()) {
            return List.of();
        }
        List<AnmatTraceabilityInconsistencyResponse> issues = new java.util.ArrayList<>();
        boolean seenReceipt = false;
        boolean currentlyDispensed = false;
        int dispenseCount = 0;
        int returnCount = 0;

        for (AnmatTraceabilityEvent event : timeline) {
            if (event.getEventType() == AnmatEventType.RECEIPT) {
                seenReceipt = true;
            }
            if (event.getEventType() == AnmatEventType.DISPENSE) {
                dispenseCount++;
                if (!seenReceipt) {
                    issues.add(issue(event, "high", "DISPENSE_WITHOUT_RECEIPT", "Dispense was recorded before any receipt event", timeline));
                }
                if (currentlyDispensed) {
                    issues.add(issue(event, "high", "DOUBLE_DISPENSE", "Serial was dispensed more than once without a return", timeline));
                }
                currentlyDispensed = true;
            }
            if (event.getEventType() == AnmatEventType.RETURN) {
                returnCount++;
                if (!currentlyDispensed) {
                    issues.add(issue(event, "medium", "RETURN_WITHOUT_DISPENSE", "Return was recorded without an active prior dispense", timeline));
                }
                currentlyDispensed = false;
            }
        }

        if (!seenReceipt) {
            AnmatTraceabilityEvent latest = timeline.getLast();
            issues.add(issue(latest, "medium", "MISSING_RECEIPT", "Serial has operational events but no receipt in history", timeline));
        }
        if (returnCount > dispenseCount) {
            AnmatTraceabilityEvent latest = timeline.getLast();
            issues.add(issue(latest, "medium", "EXCESS_RETURNS", "Serial has more returns than dispenses", timeline));
        }
        return issues;
    }

    private Map<String, List<AnmatTraceabilityEvent>> groupBySerial(List<AnmatTraceabilityEvent> events) {
        return events.stream().collect(Collectors.groupingBy(event -> event.getGtin() + "|" + event.getSerialNumber()));
    }

    private AnmatTraceabilityInconsistencyResponse issue(AnmatTraceabilityEvent event,
                                                         String severity,
                                                         String issueCode,
                                                         String message,
                                                         List<AnmatTraceabilityEvent> timeline) {
        return new AnmatTraceabilityInconsistencyResponse(
                event.getGtin(),
                event.getSerialNumber(),
                event.getLotNumber(),
                severity,
                issueCode,
                message,
                recommendationFor(issueCode),
                timeline.stream().map(value -> value.getEventType().name()).toList()
        );
    }

    private boolean ensureRemediationCase(AnmatTraceabilityInconsistencyResponse inconsistency) {
        return remediationCaseRepository.findByGtinAndSerialNumberAndIssueCode(
                inconsistency.gtin(),
                inconsistency.serialNumber(),
                inconsistency.issueCode()
        ).map(existing -> false).orElseGet(() -> {
            AnmatRemediationCase value = new AnmatRemediationCase();
            value.setGtin(inconsistency.gtin());
            value.setSerialNumber(inconsistency.serialNumber());
            value.setLotNumber(inconsistency.lotNumber());
            value.setIssueCode(inconsistency.issueCode());
            value.setSeverity(inconsistency.severity());
            value.setRecommendation(inconsistency.recommendation());
            value.setStatus(AnmatRemediationStatus.OPEN);
            remediationCaseRepository.save(value);
            return true;
        });
    }

    private String recommendationFor(String issueCode) {
        return switch (issueCode) {
            case "DISPENSE_WITHOUT_RECEIPT", "MISSING_RECEIPT" ->
                    "Review inventory evidence and create or justify the missing RECEIPT before further operations.";
            case "DOUBLE_DISPENSE" ->
                    "Block further movement on the serial and verify whether a RETURN or duplicate scan correction is needed.";
            case "RETURN_WITHOUT_DISPENSE", "EXCESS_RETURNS" ->
                    "Review store workflow and confirm whether a DISPENSE event is missing before accepting the return.";
            default ->
                    "Review the serial timeline manually and document the corrective action.";
        };
    }

    private void validateRemediationTransition(AnmatRemediationCase remediationCase,
                                               AnmatRemediationActionRequest request) {
        boolean reopening = remediationCase.getStatus() == AnmatRemediationStatus.RESOLVED
                && request.status() == AnmatRemediationStatus.OPEN;
        if (request.status() == AnmatRemediationStatus.JUSTIFIED && isBlank(request.reason())) {
            throw new BadRequestException("JUSTIFIED status requires a reason");
        }
        if (request.status() == AnmatRemediationStatus.RESOLVED && isBlank(request.reason())) {
            throw new BadRequestException("RESOLVED status requires a reason");
        }
        if (reopening && isBlank(request.reason())) {
            throw new BadRequestException("Reopening a remediation case requires a reason");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private String normalizeSortBy(String sortBy) {
        String candidate = isBlank(sortBy) ? "updatedAt" : sortBy;
        Set<String> allowed = Set.of(
                "createdAt",
                "updatedAt",
                "severity",
                "status",
                "issueCode",
                "assignedTo",
                "gtin",
                "serialNumber"
        );
        if (!allowed.contains(candidate)) {
            throw new BadRequestException("Unsupported sortBy value");
        }
        return candidate;
    }

    private Sort.Direction normalizeSortDirection(String sortDirection) {
        if (isBlank(sortDirection)) {
            return Sort.Direction.DESC;
        }
        try {
            return Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported sortDirection value");
        }
    }

    private Specification<AnmatRemediationCase> remediationCaseSpecification(AnmatRemediationStatus status,
                                                                             String assignedTo,
                                                                             String severity,
                                                                             String issueCode,
                                                                             String gtin,
                                                                             String serialNumber) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (!isBlank(assignedTo)) {
                predicates.add(criteriaBuilder.equal(root.get("assignedTo"), assignedTo));
            }
            if (!isBlank(severity)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("severity")), severity.toLowerCase()));
            }
            if (!isBlank(issueCode)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("issueCode")), issueCode.toLowerCase()));
            }
            if (!isBlank(gtin)) {
                predicates.add(criteriaBuilder.equal(root.get("gtin"), gtin));
            }
            if (!isBlank(serialNumber)) {
                predicates.add(criteriaBuilder.equal(root.get("serialNumber"), serialNumber));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"");
    }
}
