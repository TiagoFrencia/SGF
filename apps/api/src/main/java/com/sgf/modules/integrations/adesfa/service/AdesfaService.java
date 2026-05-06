package com.sgf.modules.integrations.adesfa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.modules.audit.service.AuditService;
import com.sgf.modules.core.NotFoundException;
import com.sgf.modules.integrations.adesfa.domain.AdesfaValidation;
import com.sgf.modules.integrations.adesfa.domain.AdesfaValidationRepository;
import com.sgf.modules.integrations.adesfa.domain.AdesfaValidationStatus;
import com.sgf.modules.integrations.adesfa.web.AdesfaValidationRequest;
import com.sgf.modules.integrations.adesfa.web.AdesfaValidationResponse;
import com.sgf.modules.integrations.service.OutboxService;
import com.sgf.modules.sales.domain.Sale;
import com.sgf.modules.sales.domain.SaleRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdesfaService {

    private final AdesfaProperties properties;
    private final SaleRepository saleRepository;
    private final AdesfaValidationRepository validationRepository;
    private final AdesfaGateway gateway;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public AdesfaService(AdesfaProperties properties,
                         SaleRepository saleRepository,
                         AdesfaValidationRepository validationRepository,
                         AdesfaGateway gateway,
                         AuditService auditService,
                         OutboxService outboxService,
                         ObjectMapper objectMapper) {
        this.properties = properties;
        this.saleRepository = saleRepository;
        this.validationRepository = validationRepository;
        this.gateway = gateway;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdesfaHealthResponse health() {
        String status = !properties.enabled()
                ? "DISABLED"
                : properties.mode() == AdesfaMode.SANDBOX ? "SANDBOX_READY" : "PRODUCTION_CONFIGURED";
        return new AdesfaHealthResponse(
                properties.enabled(),
                properties.mode().name(),
                properties.baseUrl(),
                properties.validationPath(),
                status
        );
    }

    @Transactional
    public AdesfaValidationResponse validateSale(UUID saleId, AdesfaValidationRequest request, String actorUsername) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found"));

        String validatorCode = request.validatorCode() == null || request.validatorCode().isBlank()
                ? properties.defaultValidatorCode()
                : request.validatorCode();

        AdesfaValidationCommand command = new AdesfaValidationCommand(
                sale.getId(),
                validatorCode,
                request.actionCode(),
                request.affiliateNumber(),
                request.prescriptionNumber(),
                sale.getTotalAmount(),
                OffsetDateTime.now()
        );

        AdesfaGateway.GatewayResult result = gateway.validate(command);

        AdesfaValidation validation = new AdesfaValidation();
        validation.setSale(sale);
        validation.setValidatorCode(validatorCode);
        validation.setActionCode(request.actionCode());
        validation.setAffiliateNumber(request.affiliateNumber());
        validation.setPrescriptionNumber(request.prescriptionNumber());
        validation.setStatus(result.success() ? AdesfaValidationStatus.APPROVED : AdesfaValidationStatus.ERROR);
        validation.setTotalAmount(sale.getTotalAmount());
        validation.setPatientAmount(defaultAmount(result.patientAmount()));
        validation.setCoverageAmount(defaultAmount(result.coverageAmount()));
        validation.setValidatedAt(command.requestedAt());
        validation.setRequestJson(toJson(buildRequestPayload(command)));
        validation.setResponseJson(result.payload());
        validation.setErrorMessage(result.errorMessage());
        validation.setProviderReference(result.providerReference());
        validation.setIntegrationMode(result.integrationMode());
        validation.setLastHttpStatus(result.httpStatus());
        validation.setRetryable(result.retryable());
        AdesfaValidation saved = validationRepository.save(validation);

        auditService.record(actorUsername, "ADESFA_VALIDATION_RECORDED", "ADESFA_VALIDATION", saved.getId(),
                "{\"saleId\":\"" + sale.getId() + "\",\"status\":\"" + saved.getStatus() + "\"}");
        outboxService.enqueue("ADESFA_VALIDATION", saved.getId(), "ADESFA_VALIDATION_" + saved.getStatus().name(),
                "{\"saleId\":\"" + sale.getId() + "\",\"validatorCode\":\"" + saved.getValidatorCode() + "\"}");
        return AdesfaValidationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AdesfaValidationResponse getValidation(UUID id) {
        return validationRepository.findById(id)
                .map(AdesfaValidationResponse::from)
                .orElseThrow(() -> new NotFoundException("ADESFA validation not found"));
    }

    @Transactional(readOnly = true)
    public List<AdesfaValidationResponse> latest() {
        return validationRepository.findTop50ByOrderByValidatedAtDesc().stream()
                .map(AdesfaValidationResponse::from)
                .toList();
    }

    private Map<String, Object> buildRequestPayload(AdesfaValidationCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("validatorCode", command.validatorCode());
        payload.put("actionCode", command.actionCode());
        payload.put("affiliateNumber", command.affiliateNumber());
        payload.put("prescriptionNumber", command.prescriptionNumber());
        payload.put("saleId", command.saleId());
        payload.put("totalAmount", command.totalAmount());
        payload.put("requestedAt", command.requestedAt());
        payload.put("softwareCode", properties.softwareCode());
        payload.put("providerCode", properties.providerCode());
        return payload;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize ADESFA payload", ex);
        }
    }
}
