package com.sgf.integrations.afip;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.audit.service.AuditService;
import com.sgf.core.domain.BadRequestException;
import com.sgf.core.domain.NotFoundException;
import com.sgf.integrations.afip.domain.AfipDocumentType;
import com.sgf.integrations.afip.domain.AfipInvoice;
import com.sgf.integrations.afip.domain.AfipInvoiceRepository;
import com.sgf.integrations.afip.domain.AfipInvoiceStatus;
import com.sgf.integrations.afip.domain.AfipInvoiceType;
import com.sgf.integrations.afip.service.*;
import com.sgf.integrations.afip.web.AfipAuthorizeInvoiceRequest;
import com.sgf.integrations.afip.web.AfipInvoiceResponse;
import com.sgf.integrations.service.OutboxService;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AfipInvoiceServiceTest {

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

    private AfipService afipService;

    @BeforeEach
    void setUp() throws Exception {
        when(properties.enabled()).thenReturn(true);
        when(properties.cuit()).thenReturn("20123456789");
        when(properties.pointOfSale()).thenReturn(1);
        when(properties.mode()).thenReturn(AfipMode.SANDBOX);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        List<AfipAuthorizationProvider> providers = List.of(sandboxProvider);
        afipService = new AfipService(
            afipInvoiceRepository,
            saleRepository,
            providers,
            properties,
            objectMapper,
            auditService,
            outboxService
        );
    }

    @Test
    void authorizesFacturaBSuccessfully() {
        // Arrange
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("1250.00"));
        
        AfipAuthorizeInvoiceRequest request = new AfipAuthorizeInvoiceRequest(
            AfipInvoiceType.FACTURA_B,
            AfipDocumentType.DNI,
            "30111222",
            "ARS",
            null
        );
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        
        AfipAuthorizationResult mockResult = new AfipAuthorizationResult(
            AfipInvoiceStatus.AUTHORIZED,
            1L,
            1L,
            "CAE_123456789",
            OffsetDateTime.now().plusDays(5),
            "REF_AFIP_001",
            "OK",
            List.of(),
            List.of(),
            OffsetDateTime.now().plusHours(12),
            "{\"cae\":\"CAE_123456789\"}"
        );
        when(sandboxProvider.authorize(any(AfipAuthorizationCommand.class))).thenReturn(mockResult);
        when(afipInvoiceRepository.save(any(AfipInvoice.class))).thenAnswer(invocation -> {
            AfipInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            return invoice;
        });

        // Act
        AfipInvoiceResponse response = afipService.authorizeSaleInvoice(saleId, request, "test_user");

        // Assert
        assertNotNull(response);
        assertEquals(AfipInvoiceStatus.AUTHORIZED, response.status());
        assertEquals("CAE_123456789", response.cae());
        verify(auditService).record(eq("test_user"), eq("AFIP_INVOICE_AUTHORIZED"), any(), any(), any());
        verify(outboxService).enqueue(eq("AFIP_INVOICE"), any(), eq("AFIP_INVOICE_AUTHORIZED"), any());
    }

    @Test
    void rejectsInvoiceWhenAfipIntegrationDisabled() {
        // Arrange
        when(properties.enabled()).thenReturn(false);
        UUID saleId = UUID.randomUUID();
        AfipAuthorizeInvoiceRequest request = createDefaultRequest();

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> afipService.authorizeSaleInvoice(saleId, request, "user")
        );
        assertEquals("AFIP integration is disabled", exception.getMessage());
    }

    @Test
    void rejectsInvoiceForNonCompletedSale() {
        // Arrange
        UUID saleId = UUID.randomUUID();
        Sale pendingSale = createSaleWithStatus(saleId, "PENDING", new BigDecimal("100.00"));
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(pendingSale));
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        
        AfipAuthorizeInvoiceRequest request = createDefaultRequest();

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> afipService.authorizeSaleInvoice(saleId, request, "user")
        );
        assertEquals("Only completed sales can be invoiced", exception.getMessage());
    }

    @Test
    void returnsExistingInvoiceWhenAlreadyAuthorized() {
        // Arrange
        UUID saleId = UUID.randomUUID();
        AfipInvoice existingInvoice = createAuthorizedInvoice(saleId);
        
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.of(existingInvoice));
        
        AfipAuthorizeInvoiceRequest request = createDefaultRequest();

        // Act
        AfipInvoiceResponse response = afipService.authorizeSaleInvoice(saleId, request, "user");

        // Assert
        assertNotNull(response);
        assertEquals(AfipInvoiceStatus.AUTHORIZED, response.status());
        assertEquals("CAE_EXISTING", response.cae());
        verify(saleRepository, never()).findById(any());
        verify(sandboxProvider, never()).authorize(any());
    }

    @Test
    void retriesOnTemporaryErrorAndSucceeds() {
        // Arrange
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("500.00"));
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        
        // First two calls fail with retryable error, third succeeds
        AfipProviderException retryableError = new AfipProviderException("WSFE_TIMEOUT", "Timeout", true, null);
        when(sandboxProvider.authorize(any()))
            .thenThrow(retryableError)
            .thenThrow(retryableError)
            .thenReturn(createSuccessfulResult());
        
        when(afipInvoiceRepository.save(any(AfipInvoice.class))).thenAnswer(invocation -> {
            AfipInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            return invoice;
        });

        // Act
        AfipInvoiceResponse response = afipService.authorizeSaleInvoice(
            saleId, 
            createDefaultRequest(), 
            "retry_user"
        );

        // Assert
        assertNotNull(response);
        assertEquals(AfipInvoiceStatus.AUTHORIZED, response.status());
        verify(sandboxProvider, times(3)).authorize(any());
    }

    @Test
    void failsAfterMaxRetriesOnPersistentError() {
        // Arrange
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("750.00"));
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        
        AfipProviderException nonRetryableError = new AfipProviderException(
            "WSFE_INVALID_CUIT", 
            "CUIT inválido", 
            false, 
            null
        );
        when(sandboxProvider.authorize(any())).thenThrow(nonRetryableError);
        
        ArgumentCaptor<AfipInvoice> invoiceCaptor = ArgumentCaptor.forClass(AfipInvoice.class);
        when(afipInvoiceRepository.save(invoiceCaptor.capture())).thenAnswer(invocation -> {
            AfipInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            return invoice;
        });

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> afipService.authorizeSaleInvoice(saleId, createDefaultRequest(), "user")
        );
        assertEquals("CUIT inválido", exception.getMessage());
        
        AfipInvoice savedInvoice = invoiceCaptor.getValue();
        assertEquals(AfipInvoiceStatus.ERROR, savedInvoice.getStatus());
        assertEquals("WSFE_INVALID_CUIT", savedInvoice.getLastErrorCode());
        verify(auditService).record(eq("user"), eq("AFIP_INVOICE_ERROR"), any(), any(), any());
    }

    @Test
    void findsExistingInvoiceById() {
        // Arrange
        UUID invoiceId = UUID.randomUUID();
        AfipInvoice invoice = createAuthorizedInvoice(UUID.randomUUID());
        invoice.setId(invoiceId);
        
        when(afipInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // Act
        AfipInvoiceResponse response = afipService.getInvoice(invoiceId);

        // Assert
        assertNotNull(response);
        assertEquals(AfipInvoiceStatus.AUTHORIZED, response.status());
    }

    @Test
    void throwsNotFoundWhenInvoiceDoesNotExist() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(afipInvoiceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            NotFoundException.class,
            () -> afipService.getInvoice(nonExistentId)
        );
    }

    @Test
    void handlesNotaCreditoSuccessfully() {
        // Arrange
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("-300.00"));
        
        AfipAuthorizeInvoiceRequest request = new AfipAuthorizeInvoiceRequest(
            AfipInvoiceType.NOTA_CREDITO_B,
            AfipDocumentType.CUIT,
            "30123456789",
            "ARS",
            null
        );
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(afipInvoiceRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(sandboxProvider.authorize(any())).thenReturn(createSuccessfulResult());
        when(afipInvoiceRepository.save(any(AfipInvoice.class))).thenAnswer(invocation -> {
            AfipInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            return invoice;
        });

        // Act
        AfipInvoiceResponse response = afipService.authorizeSaleInvoice(saleId, request, "user");

        // Assert
        assertNotNull(response);
        assertEquals(AfipInvoiceStatus.AUTHORIZED, response.status());
        ArgumentCaptor<AfipAuthorizationCommand> commandCaptor = ArgumentCaptor.forClass(AfipAuthorizationCommand.class);
        verify(sandboxProvider).authorize(commandCaptor.capture());
        assertEquals(AfipInvoiceType.NOTA_CREDITO_B, commandCaptor.getValue().invoiceType());
    }

    private Sale createCompletedSale(UUID id, BigDecimal amount) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setStatus("COMPLETED");
        sale.setTotalAmount(amount);
        return sale;
    }

    private Sale createSaleWithStatus(UUID id, String status, BigDecimal amount) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setStatus(status);
        sale.setTotalAmount(amount);
        return sale;
    }

    private AfipInvoice createAuthorizedInvoice(UUID saleId) {
        AfipInvoice invoice = new AfipInvoice();
        invoice.setId(UUID.randomUUID());
        invoice.setSale(new Sale());
        invoice.getSale().setId(saleId);
        invoice.setStatus(AfipInvoiceStatus.AUTHORIZED);
        invoice.setCae("CAE_EXISTING");
        invoice.setPointOfSale(1);
        invoice.setInvoiceType(AfipInvoiceType.FACTURA_A);
        invoice.setCustomerDocumentType(AfipDocumentType.DNI);
        invoice.setCustomerDocumentNumber("30111222");
        invoice.setCurrencyCode("ARS");
        invoice.setNetAmount(new BigDecimal("100.00"));
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setRetryCount(0);
        invoice.setRequestJson("{}");
        return invoice;
    }

    private AfipAuthorizeInvoiceRequest createDefaultRequest() {
        return new AfipAuthorizeInvoiceRequest(
            AfipInvoiceType.FACTURA_A,
            AfipDocumentType.DNI,
            "30111222",
            "ARS",
            null
        );
    }

    private AfipAuthorizationResult createSuccessfulResult() {
        return new AfipAuthorizationResult(
            AfipInvoiceStatus.AUTHORIZED,
            1L,
            1L,
            "CAE_TEST_123",
            OffsetDateTime.now().plusDays(5),
            "REF_TEST",
            "OK",
            List.of(),
            List.of(),
            OffsetDateTime.now().plusHours(12),
            "{\"status\":\"approved\"}"
        );
    }
}
