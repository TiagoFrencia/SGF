package com.sgf.integrations.adesfa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.audit.service.AuditService;
import com.sgf.core.domain.NotFoundException;
import com.sgf.integrations.adesfa.domain.AdesfaValidation;
import com.sgf.integrations.adesfa.domain.AdesfaValidationRepository;
import com.sgf.integrations.adesfa.domain.AdesfaValidationStatus;
import com.sgf.integrations.adesfa.service.*;
import com.sgf.integrations.adesfa.web.AdesfaValidationRequest;
import com.sgf.integrations.adesfa.web.AdesfaValidationResponse;
import com.sgf.integrations.service.OutboxService;
import com.sgf.pos.domain.Sale;
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
class AdesfaValidationServiceTest {

    @Mock
    private AdesfaProperties properties;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private AdesfaValidationRepository validationRepository;

    @Mock
    private AdesfaGateway gateway;

    @Mock
    private AuditService auditService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ObjectMapper objectMapper;

    private AdesfaService adesfaService;

    @BeforeEach
    void setUp() throws Exception {
        when(properties.enabled()).thenReturn(true);
        when(properties.mode()).thenReturn(AdesfaMode.SANDBOX);
        when(properties.defaultValidatorCode()).thenReturn("PAMI");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        adesfaService = new AdesfaService(
            properties,
            saleRepository,
            validationRepository,
            gateway,
            auditService,
            outboxService,
            objectMapper
        );
    }

    @Test
    void validatesPamiSaleSuccessfully() {
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("1000.00"));
        
        AdesfaValidationRequest request = new AdesfaValidationRequest(
            "PAMI", "ACC001", "123456789", "REC001", List.of()
        );
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(validationRepository.save(any(AdesfaValidation.class))).thenAnswer(invocation -> {
            AdesfaValidation validation = invocation.getArgument(0);
            validation.setId(UUID.randomUUID());
            return validation;
        });
        when(gateway.validate(any(AdesfaValidationCommand.class))).thenReturn(
            new AdesfaValidationResult(true, "APPROVED", "Validación PAMI Exitosa",
                new BigDecimal("700.00"), new BigDecimal("300.00"), "REF_PAMI_001")
        );

        AdesfaValidationResponse response = adesfaService.validateSale(saleId, request, "pharmacist");

        assertNotNull(response);
        assertEquals(AdesfaValidationStatus.APPROVED, response.status());
        assertEquals(new BigDecimal("700.00"), response.coverageAmount());
        verify(auditService).record(eq("pharmacist"), eq("ADESFA_VALIDATION_APPROVED"), any(), any(), any());
    }

    @Test
    void validatesOsdeSale() {
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("2000.00"));
        
        AdesfaValidationRequest request = new AdesfaValidationRequest(
            "OSDE", "ACC002", "987654321", "REC002", List.of()
        );
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(validationRepository.save(any(AdesfaValidation.class))).thenAnswer(invocation -> {
            AdesfaValidation validation = invocation.getArgument(0);
            validation.setId(UUID.randomUUID());
            return validation;
        });
        when(gateway.validate(any(AdesfaValidationCommand.class))).thenReturn(
            new AdesfaValidationResult(true, "APPROVED", "Validación OSDE 80%",
                new BigDecimal("1600.00"), new BigDecimal("400.00"), "REF_OSDE_001")
        );

        AdesfaValidationResponse response = adesfaService.validateSale(saleId, request, "pharmacist");

        assertNotNull(response);
        assertEquals(AdesfaValidationStatus.APPROVED, response.status());
        ArgumentCaptor<AdesfaValidationCommand> commandCaptor = ArgumentCaptor.forClass(AdesfaValidationCommand.class);
        verify(gateway).validate(commandCaptor.capture());
        assertEquals("OSDE", commandCaptor.getValue().validatorCode());
    }

    @Test
    void validatesSwissMedicalSale() {
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("1500.00"));
        
        AdesfaValidationRequest request = new AdesfaValidationRequest(
            "SWISS_MEDICAL", "ACC003", "555666777", "REC003", List.of()
        );
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(validationRepository.save(any(AdesfaValidation.class))).thenAnswer(invocation -> {
            AdesfaValidation validation = invocation.getArgument(0);
            validation.setId(UUID.randomUUID());
            return validation;
        });
        when(gateway.validate(any(AdesfaValidationCommand.class))).thenReturn(
            new AdesfaValidationResult(true, "APPROVED", "Swiss Medical OK",
                new BigDecimal("1200.00"), new BigDecimal("300.00"), "REF_SWISS_001")
        );

        AdesfaValidationResponse response = adesfaService.validateSale(saleId, request, "pharmacist");

        assertNotNull(response);
        assertEquals(AdesfaValidationStatus.APPROVED, response.status());
    }

    @Test
    void rejectsValidationWhenSaleNotFound() {
        UUID nonExistentSaleId = UUID.randomUUID();
        AdesfaValidationRequest request = createDefaultRequest();
        
        when(saleRepository.findById(nonExistentSaleId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> adesfaService.validateSale(nonExistentSaleId, request, "user"));
    }

    @Test
    void handlesValidationRejection() {
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("500.00"));
        
        AdesfaValidationRequest request = new AdesfaValidationRequest(
            "PAMI", "ACC001", "INVALID_AFFILIATE", "REC001", List.of()
        );
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(validationRepository.save(any(AdesfaValidation.class))).thenAnswer(invocation -> {
            AdesfaValidation validation = invocation.getArgument(0);
            validation.setId(UUID.randomUUID());
            return validation;
        });
        when(gateway.validate(any(AdesfaValidationCommand.class))).thenReturn(
            new AdesfaValidationResult(false, "REJECTED", "Afiliado no encontrado",
                BigDecimal.ZERO, new BigDecimal("500.00"), null)
        );

        AdesfaValidationResponse response = adesfaService.validateSale(saleId, request, "pharmacist");

        assertNotNull(response);
        assertEquals(AdesfaValidationStatus.REJECTED, response.status());
        assertEquals(BigDecimal.ZERO, response.coverageAmount());
    }

    @Test
    void usesDefaultValidatorCodeWhenNotProvided() {
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("600.00"));
        
        AdesfaValidationRequest request = new AdesfaValidationRequest(
            null, "ACC001", "123456789", "REC001", List.of()
        );
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(validationRepository.save(any(AdesfaValidation.class))).thenAnswer(invocation -> {
            AdesfaValidation validation = invocation.getArgument(0);
            validation.setId(UUID.randomUUID());
            return validation;
        });
        when(gateway.validate(any(AdesfaValidationCommand.class))).thenReturn(
            new AdesfaValidationResult(true, "APPROVED", "OK",
                new BigDecimal("420.00"), new BigDecimal("180.00"), "REF_DEFAULT")
        );

        adesfaService.validateSale(saleId, request, "user");

        ArgumentCaptor<AdesfaValidationCommand> commandCaptor = ArgumentCaptor.forClass(AdesfaValidationCommand.class);
        verify(gateway).validate(commandCaptor.capture());
        assertEquals("PAMI", commandCaptor.getValue().validatorCode());
    }

    @Test
    void handlesHighCostTreatmentWithFullCoverage() {
        UUID saleId = UUID.randomUUID();
        Sale sale = createCompletedSale(saleId, new BigDecimal("50000.00"));
        
        AdesfaValidationRequest request = new AdesfaValidationRequest(
            "PAMI_ONCO", "ACC_ONCO", "123456789", "REC_ONCO", List.of()
        );
        
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(validationRepository.save(any(AdesfaValidation.class))).thenAnswer(invocation -> {
            AdesfaValidation validation = invocation.getArgument(0);
            validation.setId(UUID.randomUUID());
            return validation;
        });
        when(gateway.validate(any(AdesfaValidationCommand.class))).thenReturn(
            new AdesfaValidationResult(true, "APPROVED_100", "Cobertura 100%",
                new BigDecimal("50000.00"), BigDecimal.ZERO, "REF_ONCO_001")
        );

        AdesfaValidationResponse response = adesfaService.validateSale(saleId, request, "oncologist");

        assertNotNull(response);
        assertEquals(AdesfaValidationStatus.APPROVED, response.status());
        assertEquals(new BigDecimal("50000.00"), response.coverageAmount());
        assertEquals(BigDecimal.ZERO, response.patientPayAmount());
    }

    private Sale createCompletedSale(UUID id, BigDecimal amount) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setStatus("COMPLETED");
        sale.setTotalAmount(amount);
        sale.setCreatedAt(OffsetDateTime.now());
        return sale;
    }

    private AdesfaValidationRequest createDefaultRequest() {
        return new AdesfaValidationRequest("PAMI", "ACC001", "123456789", "REC001", List.of());
    }
}
