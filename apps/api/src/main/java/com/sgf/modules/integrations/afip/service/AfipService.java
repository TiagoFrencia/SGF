package com.sgf.modules.integrations.afip.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.modules.audit.service.AuditService;
import com.sgf.modules.core.BadRequestException;
import com.sgf.modules.core.NotFoundException;
import com.sgf.modules.integrations.afip.domain.AfipInvoice;
import com.sgf.modules.integrations.afip.domain.AfipInvoiceRepository;
import com.sgf.modules.integrations.afip.domain.AfipInvoiceStatus;
import com.sgf.modules.integrations.afip.web.AfipAuthorizeInvoiceRequest;
import com.sgf.modules.integrations.afip.web.AfipInvoiceResponse;
import com.sgf.modules.integrations.service.OutboxService;
import com.sgf.modules.sales.domain.Sale;
import com.sgf.modules.sales.domain.SaleRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AfipService {

    private final AfipInvoiceRepository afipInvoiceRepository;
    private final SaleRepository saleRepository;
    private final List<AfipAuthorizationProvider> providers;
    private final AfipProperties properties;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final OutboxService outboxService;

    public AfipService(AfipInvoiceRepository afipInvoiceRepository,
                       SaleRepository saleRepository,
                       List<AfipAuthorizationProvider> providers,
                       AfipProperties properties,
                       ObjectMapper objectMapper,
                       AuditService auditService,
                       OutboxService outboxService) {
        this.afipInvoiceRepository = afipInvoiceRepository;
        this.saleRepository = saleRepository;
        this.providers = providers;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.outboxService = outboxService;
    }

    @Transactional
    public AfipInvoiceResponse authorizeSaleInvoice(java.util.UUID saleId,
                                                    AfipAuthorizeInvoiceRequest request,
                                                    String actorUsername) {
        if (!properties.enabled()) {
            throw new BadRequestException("AFIP integration is disabled");
        }

        return afipInvoiceRepository.findBySaleId(saleId)
                .map(existing -> reuseOrRetry(existing, request, actorUsername))
                .orElseGet(() -> createInvoice(saleId, request, actorUsername));
    }

    @Transactional(readOnly = true)
    public AfipInvoiceResponse getInvoice(java.util.UUID invoiceId) {
        return afipInvoiceRepository.findById(invoiceId)
                .map(AfipInvoiceResponse::from)
                .orElseThrow(() -> new NotFoundException("AFIP invoice not found"));
    }

    private AfipInvoiceResponse createInvoice(java.util.UUID saleId,
                                              AfipAuthorizeInvoiceRequest request,
                                              String actorUsername) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found"));
        if (!"COMPLETED".equalsIgnoreCase(sale.getStatus())) {
            throw new BadRequestException("Only completed sales can be invoiced");
        }

        BigDecimal total = sale.getTotalAmount();
        AfipAuthorizationCommand command = new AfipAuthorizationCommand(
                sale.getId(),
                properties.cuit(),
                request.pointOfSale() != null ? request.pointOfSale() : properties.pointOfSale(),
                request.invoiceType(),
                request.customerDocumentType(),
                request.customerDocumentNumber(),
                total,
                request.currencyCode(),
                1L,
                1L
        );
        AfipAuthorizationProvider provider = providers.stream()
                .filter(candidate -> candidate.mode() == properties.mode())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No AFIP provider for mode " + properties.mode()));

        AfipInvoice invoice = new AfipInvoice();
        invoice.setSale(sale);
        invoice.setPointOfSale(command.pointOfSale());
        invoice.setInvoiceType(command.invoiceType());
        invoice.setCustomerDocumentType(command.customerDocumentType());
        invoice.setCustomerDocumentNumber(command.customerDocumentNumber());
        invoice.setCurrencyCode(command.currencyCode());
        invoice.setNetAmount(total);
        invoice.setTotalAmount(total);
        invoice.setStatus(AfipInvoiceStatus.PENDING);
        invoice.setRetryCount(0);
        invoice.setRequestJson(toJson(Map.of(
                "saleId", sale.getId(),
                "cuit", command.cuit(),
                "pointOfSale", command.pointOfSale(),
                "invoiceType", command.invoiceType(),
                "customerDocumentType", command.customerDocumentType(),
                "customerDocumentNumber", command.customerDocumentNumber(),
                "currencyCode", command.currencyCode(),
                "totalAmount", command.totalAmount()
        )));
        AfipInvoice pending = afipInvoiceRepository.save(invoice);
        return runAuthorization(pending, command, provider, actorUsername);
    }

    private AfipInvoiceResponse reuseOrRetry(AfipInvoice existing,
                                             AfipAuthorizeInvoiceRequest request,
                                             String actorUsername) {
        if (existing.getStatus() == AfipInvoiceStatus.AUTHORIZED || existing.getStatus() == AfipInvoiceStatus.REJECTED) {
            return AfipInvoiceResponse.from(existing);
        }
        if (existing.getStatus() == AfipInvoiceStatus.PENDING
                && existing.getLastAttemptedAt() != null
                && existing.getLastAttemptedAt().isAfter(OffsetDateTime.now().minusMinutes(2))) {
            return AfipInvoiceResponse.from(existing);
        }
        if (existing.getStatus() == AfipInvoiceStatus.ERROR
                && existing.getLastErrorCode() != null
                && existing.getLastErrorCode().startsWith("WSFE_")) {
            return AfipInvoiceResponse.from(existing);
        }
        AfipAuthorizationCommand command = new AfipAuthorizationCommand(
                existing.getSale().getId(),
                properties.cuit(),
                existing.getPointOfSale(),
                existing.getInvoiceType(),
                existing.getCustomerDocumentType(),
                existing.getCustomerDocumentNumber(),
                existing.getTotalAmount(),
                existing.getCurrencyCode(),
                existing.getVoucherNumberFrom() != null ? existing.getVoucherNumberFrom() : 1L,
                existing.getVoucherNumberTo() != null ? existing.getVoucherNumberTo() : 1L
        );
        AfipAuthorizationProvider provider = providers.stream()
                .filter(candidate -> candidate.mode() == properties.mode())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No AFIP provider for mode " + properties.mode()));
        return runAuthorization(existing, command, provider, actorUsername);
    }

    private AfipInvoiceResponse runAuthorization(AfipInvoice invoice,
                                                 AfipAuthorizationCommand command,
                                                 AfipAuthorizationProvider provider,
                                                 String actorUsername) {
        invoice.setStatus(AfipInvoiceStatus.PENDING);
        invoice.setLastAttemptedAt(OffsetDateTime.now());
        invoice.setLastErrorCode(null);
        invoice.setLastErrorMessage(null);
        AfipAuthorizationResult result = null;
        AfipProviderException lastException = null;
        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            try {
                result = provider.authorize(command);
                lastException = null;
                break;
            } catch (AfipProviderException ex) {
                lastException = ex;
                if (!ex.isRetryable() || attempts >= 3) {
                    break;
                }
            }
        }

        invoice.setRetryCount((invoice.getRetryCount() == null ? 0 : invoice.getRetryCount()) + attempts);

        if (result != null) {
            applySuccess(invoice, result);
            AfipInvoice saved = afipInvoiceRepository.save(invoice);
            auditService.record(actorUsername, "AFIP_INVOICE_AUTHORIZED", "AFIP_INVOICE", saved.getId(),
                    "{\"saleId\":\"" + saved.getSale().getId() + "\",\"mode\":\"" + properties.mode() + "\",\"status\":\"" + saved.getStatus() + "\"}");
            outboxService.enqueue("AFIP_INVOICE", saved.getId(), "AFIP_INVOICE_" + saved.getStatus().name(),
                    "{\"saleId\":\"" + saved.getSale().getId() + "\",\"status\":\"" + saved.getStatus() + "\"}");
            return AfipInvoiceResponse.from(saved);
        }

        applyFailure(invoice, lastException);
        AfipInvoice saved = afipInvoiceRepository.save(invoice);
        auditService.record(actorUsername, "AFIP_INVOICE_ERROR", "AFIP_INVOICE", saved.getId(),
                "{\"saleId\":\"" + saved.getSale().getId() + "\",\"errorCode\":\"" + saved.getLastErrorCode() + "\"}");
        throw new BadRequestException(saved.getLastErrorMessage());
    }

    private void applySuccess(AfipInvoice invoice, AfipAuthorizationResult result) {
        invoice.setStatus(result.status());
        invoice.setVoucherNumberFrom(result.voucherNumberFrom());
        invoice.setVoucherNumberTo(result.voucherNumberTo());
        invoice.setAfipResultCode(result.afipResultCode());
        invoice.setCae(result.cae());
        invoice.setCaeDueDate(result.caeDueDate());
        invoice.setProviderReference(result.providerReference());
        invoice.setResponseJson(result.responseJson());
        invoice.setObservationsJson(toJson(result.observations()));
        invoice.setErrorsJson(toJson(result.errors()));
        invoice.setLastErrorCode(null);
        invoice.setLastErrorMessage(null);
        invoice.setTokenExpiresAt(result.tokenExpiresAt());
        if (result.status() == AfipInvoiceStatus.AUTHORIZED) {
            invoice.setAuthorizedAt(OffsetDateTime.now());
        }
    }

    private void applyFailure(AfipInvoice invoice, AfipProviderException exception) {
        invoice.setStatus(AfipInvoiceStatus.ERROR);
        invoice.setLastErrorCode(exception != null ? exception.getCode() : "UNKNOWN");
        invoice.setLastErrorMessage(exception != null ? exception.getMessage() : "Unknown AFIP error");
        invoice.setResponseJson(exception != null ? exception.getResponsePayload() : null);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize AFIP payload", ex);
        }
    }
}
