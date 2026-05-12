package com.sgf.integrations.afip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.audit.service.AuditService;
import com.sgf.core.domain.BadRequestException;
import com.sgf.core.domain.NotFoundException;
import com.sgf.integrations.afip.domain.AfipDocumentType;
import com.sgf.integrations.afip.domain.AfipInvoice;
import com.sgf.integrations.afip.domain.AfipInvoiceRepository;
import com.sgf.integrations.afip.domain.AfipInvoiceStatus;
import com.sgf.integrations.afip.domain.AfipInvoiceType;
import com.sgf.integrations.afip.web.AfipAuthorizeInvoiceRequest;
import com.sgf.integrations.afip.web.AfipInvoiceResponse;
import com.sgf.integrations.service.OutboxService;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AfipServiceTest {

    @Mock private AfipInvoiceRepository afipInvoiceRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private AfipAuthorizationProvider sandboxProvider;
    @Mock private AfipProperties properties;
    @Mock private ObjectMapper objectMapper;
    @Mock private AuditService auditService;
    @Mock private OutboxService outboxService;

    private AfipService afipService;

    @BeforeEach
    void setUp() {
        afipService = new AfipService(
                afipInvoiceRepository,
                saleRepository,
                List.of(sandboxProvider),
                properties,
                objectMapper,
                auditService,
                outboxService
        );
    }

    @Test
    void shouldThrowWhenAfipIntegrationDisabled() {
        when(properties.enabled()).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> afipService.authorizeSaleInvoice(UUID.randomUUID(), createValidRequest(), "testUser"));
    }

    @Test
    void shouldReuseExistingAuthorizedInvoice() {
        UUID saleId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        AfipInvoice existing = createExistingInvoice(invoiceId, saleId);
        existing.setStatus(AfipInvoiceStatus.AUTHORIZED);

        when(properties.enabled()).thenReturn(true);
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.of(existing));

        AfipInvoiceResponse response = afipService.authorizeSaleInvoice(saleId, createValidRequest(), "testUser");

        assertEquals(invoiceId, response.id());
        verify(saleRepository, never()).findById(any());
        verify(sandboxProvider, never()).authorize(any());
    }

    @Test
    void shouldAuthorizeNewInvoiceSuccessfully() throws Exception {
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("1000.00"));

        when(properties.enabled()).thenReturn(true);
        when(properties.cuit()).thenReturn("30123456789");
        when(properties.mode()).thenReturn(AfipMode.SANDBOX);
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(sandboxProvider.mode()).thenReturn(AfipMode.SANDBOX);
        when(sandboxProvider.authorize(any())).thenReturn(new AfipAuthorizationResult(
                AfipInvoiceStatus.AUTHORIZED,
                12L,
                12L,
                "A",
                "7289384732984723",
                LocalDate.now().plusDays(5),
                "REF-AFIP",
                "{\"ok\":true}",
                List.of(),
                List.of(),
                null
        ));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(afipInvoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AfipInvoiceResponse response = afipService.authorizeSaleInvoice(saleId, createValidRequest(), "testUser");

        assertNotNull(response);
        assertEquals("7289384732984723", response.cae());
        assertEquals("AUTHORIZED", response.status());
        verify(auditService).record(eq("testUser"), eq("AFIP_INVOICE_AUTHORIZED"), any(), any(), any());
        verify(outboxService).enqueue(eq("AFIP_INVOICE"), any(), eq("AFIP_INVOICE_AUTHORIZED"), any());
    }

    @Test
    void shouldThrowWhenSaleMissing() {
        UUID saleId = UUID.randomUUID();
        when(properties.enabled()).thenReturn(true);
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> afipService.authorizeSaleInvoice(saleId, createValidRequest(), "testUser"));
    }

    @Test
    void shouldThrowWhenProviderFails() throws Exception {
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("500.00"));

        when(properties.enabled()).thenReturn(true);
        when(properties.cuit()).thenReturn("30123456789");
        when(properties.mode()).thenReturn(AfipMode.SANDBOX);
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(sandboxProvider.mode()).thenReturn(AfipMode.SANDBOX);
        when(sandboxProvider.authorize(any())).thenThrow(new AfipProviderException("WSFE_001", "Provider error", false, "{}"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(afipInvoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThrows(BadRequestException.class,
                () -> afipService.authorizeSaleInvoice(saleId, createValidRequest(), "testUser"));
        verify(auditService).record(eq("testUser"), eq("AFIP_INVOICE_ERROR"), any(), any(), any());
    }

    private AfipAuthorizeInvoiceRequest createValidRequest() {
        return new AfipAuthorizeInvoiceRequest(
                AfipInvoiceType.FACTURA_B,
                AfipDocumentType.DNI,
                "30111222",
                1,
                "ARS"
        );
    }

    private Sale createCompletedSale(UUID saleId, BigDecimal totalAmount) {
        Sale sale = new Sale();
        sale.setId(saleId);
        sale.setStatus("COMPLETED");
        sale.setTotalAmount(totalAmount);
        return sale;
    }

    private AfipInvoice createExistingInvoice(UUID invoiceId, UUID saleId) {
        AfipInvoice invoice = new AfipInvoice();
        Sale sale = new Sale();
        sale.setId(saleId);
        invoice.setId(invoiceId);
        invoice.setSale(sale);
        invoice.setPointOfSale(1);
        invoice.setInvoiceType(AfipInvoiceType.FACTURA_B);
        invoice.setCustomerDocumentType(AfipDocumentType.DNI);
        invoice.setCustomerDocumentNumber("30111222");
        invoice.setCurrencyCode("ARS");
        invoice.setNetAmount(new BigDecimal("1000.00"));
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setRetryCount(0);
        invoice.setRequestJson("{}");
        return invoice;
    }
}
