package com.sgf.integrations.afip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.audit.service.AuditService;
import com.sgf.core.domain.BadRequestException;
import com.sgf.core.domain.NotFoundException;
import com.sgf.integrations.afip.domain.AfipInvoice;
import com.sgf.integrations.afip.domain.AfipInvoiceRepository;
import com.sgf.integrations.afip.domain.AfipInvoiceStatus;
import com.sgf.integrations.afip.web.AfipAuthorizeInvoiceRequest;
import com.sgf.integrations.afip.web.AfipInvoiceResponse;
import com.sgf.integrations.service.OutboxService;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AfipService
 * Valida la autorización de facturas electrónicas ante AFIP
 */
@ExtendWith(MockitoExtension.class)
class AfipServiceTest {

    @Mock
    private AfipInvoiceRepository afipInvoiceRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private AfipAuthorizationProvider sandboxProvider;

    @Mock
    private AfipProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AfipAuthorizationResult authorizationResult;

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
    void shouldThrowException_WhenAfipIntegrationDisabled() {
        // Given
        when(properties.enabled()).thenReturn(false);
        UUID saleId = UUID.randomUUID();
        AfipAuthorizeInvoiceRequest request = createValidRequest();

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> afipService.authorizeSaleInvoice(saleId, request, "testUser")
        );
        assertEquals("AFIP integration is disabled", exception.getMessage());
    }

    @Test
    void shouldReuseExistingInvoice_WhenInvoiceAlreadyExistsForSale() {
        // Given
        when(properties.enabled()).thenReturn(true);
        UUID saleId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        AfipInvoice existingInvoice = createExistingInvoice(invoiceId, saleId);
        AfipAuthorizeInvoiceRequest request = createValidRequest();

        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.of(existingInvoice));

        // When
        AfipInvoiceResponse response = afipService.authorizeSaleInvoice(saleId, request, "testUser");

        // Then
        assertNotNull(response);
        assertEquals(invoiceId, response.id());
        verify(afipInvoiceRepository, times(1)).findBySaleId(saleId);
        verify(saleRepository, never()).findById(any());
        verify(sandboxProvider, never()).authorize(any());
    }

    @Test
    void shouldCreateNewInvoice_WhenNoExistingInvoiceForSale() {
        // Given
        when(properties.enabled()).thenReturn(true);
        when(properties.cuit()).thenReturn("30123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.mode()).thenReturn(AfipMode.SANDBOX);
        
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, BigDecimal.valueOf(1000.00));
        AfipAuthorizeInvoiceRequest request = createValidRequest();

        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(sandboxProvider.mode()).thenReturn(AfipMode.SANDBOX);
        when(sandboxProvider.authorize(any(AfipAuthorizationCommand.class))).thenReturn(authorizationResult);
        when(authorizationResult.cae()).thenReturn("7289384732984723");
        when(authorizationResult.caeExpirationDate()).thenReturn(OffsetDateTime.now().plusDays(5));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"amount\":1000}");

        // When
        AfipInvoiceResponse response = afipService.authorizeSaleInvoice(saleId, request, "testUser");

        // Then
        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("7289384732984723", response.cae());
        assertEquals(AfipInvoiceStatus.AUTHORIZED, response.status());
        
        ArgumentCaptor<AfipAuthorizationCommand> commandCaptor = ArgumentCaptor.forClass(AfipAuthorizationCommand.class);
        verify(sandboxProvider).authorize(commandCaptor.capture());
        
        AfipAuthorizationCommand capturedCommand = commandCaptor.getValue();
        assertEquals("30123456789", capturedCommand.cuit());
        assertEquals(BigDecimal.valueOf(1000.00), capturedCommand.totalAmount());
        
        verify(afipInvoiceRepository).save(any(AfipInvoice.class));
        verify(auditService).auditEvent(eq("INVOICE_AUTHORIZED"), any(), eq("testUser"), any());
    }

    @Test
    void shouldThrowException_WhenSaleNotFound() {
        // Given
        when(properties.enabled()).thenReturn(true);
        UUID saleId = UUID.randomUUID();
        AfipAuthorizeInvoiceRequest request = createValidRequest();

        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> afipService.authorizeSaleInvoice(saleId, request, "testUser")
        );
        assertEquals("Sale not found", exception.getMessage());
    }

    @Test
    void shouldThrowException_WhenSaleNotCompleted() {
        // Given
        when(properties.enabled()).thenReturn(true);
        UUID saleId = UUID.randomUUID();
        Sale pendingSale = createSaleWithStatus(saleId, "PENDING");
        AfipAuthorizeInvoiceRequest request = createValidRequest();

        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(pendingSale));

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> afipService.authorizeSaleInvoice(saleId, request, "testUser")
        );
        assertEquals("Only completed sales can be invoiced", exception.getMessage());
    }

    @Test
    void shouldThrowException_WhenNoProviderForMode() {
        // Given
        when(properties.enabled()).thenReturn(true);
        when(properties.mode()).thenReturn(AfipMode.PRODUCTION);
        
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, BigDecimal.valueOf(1000.00));
        AfipAuthorizeInvoiceRequest request = createValidRequest();

        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(sandboxProvider.mode()).thenReturn(AfipMode.SANDBOX);

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> afipService.authorizeSaleInvoice(saleId, request, "testUser")
        );
        assertTrue(exception.getMessage().contains("No AFIP provider for mode"));
    }

    @Test
    void shouldGetInvoice_WhenInvoiceExists() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        AfipInvoice invoice = createExistingInvoice(invoiceId, UUID.randomUUID());

        when(afipInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // When
        AfipInvoiceResponse response = afipService.getInvoice(invoiceId);

        // Then
        assertNotNull(response);
        assertEquals(invoiceId, response.id());
        verify(afipInvoiceRepository).findById(invoiceId);
    }

    @Test
    void shouldThrowException_WhenInvoiceNotFound() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        when(afipInvoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> afipService.getInvoice(invoiceId)
        );
        assertEquals("AFIP invoice not found", exception.getMessage());
    }

    @Test
    void shouldUseRequestPointOfSale_WhenProvidedInRequest() {
        // Given
        when(properties.enabled()).thenReturn(true);
        when(properties.cuit()).thenReturn("30123456789");
        when(properties.mode()).thenReturn(AfipMode.SANDBOX);
        
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, BigDecimal.valueOf(500.00));
        AfipAuthorizeInvoiceRequest request = new AfipAuthorizeInvoiceRequest(
            2, // pointOfSale diferente al default
            "96", // invoiceType
            "CUIT",
            "20123456789",
            "ARS",
            null
        );

        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(sandboxProvider.mode()).thenReturn(AfipMode.SANDBOX);
        when(sandboxProvider.authorize(any())).thenReturn(authorizationResult);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        afipService.authorizeSaleInvoice(saleId, request, "testUser");

        // Then
        ArgumentCaptor<AfipAuthorizationCommand> commandCaptor = ArgumentCaptor.forClass(AfipAuthorizationCommand.class);
        verify(sandboxProvider).authorize(commandCaptor.capture());
        
        assertEquals(2, commandCaptor.getValue().pointOfSale());
    }

    @Test
    void shouldAuditInvoiceCreation_WhenAuthorizedSuccessfully() {
        // Given
        when(properties.enabled()).thenReturn(true);
        when(properties.cuit()).thenReturn("30123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.mode()).thenReturn(AfipMode.SANDBOX);
        
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, BigDecimal.valueOf(1000.00));
        AfipAuthorizeInvoiceRequest request = createValidRequest();

        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(sandboxProvider.mode()).thenReturn(AfipMode.SANDBOX);
        when(sandboxProvider.authorize(any())).thenReturn(authorizationResult);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        afipService.authorizeSaleInvoice(saleId, request, "testUser");

        // Then
        verify(auditService).auditEvent(
            eq("INVOICE_AUTHORIZED"),
            any(),
            eq("testUser"),
            any()
        );
    }

    // Helper methods
    private AfipAuthorizeInvoiceRequest createValidRequest() {
        return new AfipAuthorizeInvoiceRequest(
            null, // use default point of sale
            "96", // Factura B
            "CUIT",
            "20123456789",
            "ARS",
            null
        );
    }

    private AfipInvoice createExistingInvoice(UUID invoiceId, UUID saleId) {
        AfipInvoice invoice = new AfipInvoice();
        invoice.setId(invoiceId);
        invoice.setSale(createCompletedSale(saleId, BigDecimal.valueOf(100.00)));
        invoice.setPointOfSale(1);
        invoice.setInvoiceType("96");
        invoice.setStatus(AfipInvoiceStatus.AUTHORIZED);
        invoice.setCae("1234567890");
        invoice.setCreatedAt(OffsetDateTime.now());
        return invoice;
    }

    private Sale createCompletedSale(UUID id, BigDecimal amount) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setStatus("COMPLETED");
        sale.setTotalAmount(amount);
        sale.setCreatedAt(OffsetDateTime.now());
        return sale;
    }

    private Sale createSaleWithStatus(UUID id, String status) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setStatus(status);
        sale.setTotalAmount(BigDecimal.valueOf(100.00));
        sale.setCreatedAt(OffsetDateTime.now());
        return sale;
    }
}
