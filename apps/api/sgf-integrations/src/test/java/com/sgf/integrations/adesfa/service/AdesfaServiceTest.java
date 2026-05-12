package com.sgf.integrations.adesfa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.audit.service.AuditService;
import com.sgf.core.domain.NotFoundException;
import com.sgf.integrations.adesfa.web.AdesfaValidationRequest;
import com.sgf.integrations.adesfa.web.AdesfaValidationResponse;
import com.sgf.integrations.service.OutboxService;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdesfaServiceTest {

    @Mock private AdesfaProperties properties;
    @Mock private SaleRepository saleRepository;
    @Mock private com.sgf.integrations.adesfa.domain.AdesfaValidationRepository validationRepository;
    @Mock private AdesfaGateway gateway;
    @Mock private AuditService auditService;
    @Mock private OutboxService outboxService;
    @Mock private ObjectMapper objectMapper;

    private AdesfaService service;

    @BeforeEach
    void setUp() {
        service = new AdesfaService(
                properties, saleRepository, validationRepository,
                gateway, auditService, outboxService, objectMapper
        );
    }

    @Test
    void shouldApproveValidationSuccessfully() throws Exception {
        UUID saleId = UUID.randomUUID();
        Sale sale = new Sale();
        sale.setId(saleId);
        sale.setTotalAmount(new BigDecimal("1500.00"));

        AdesfaValidationRequest request = new AdesfaValidationRequest(
                "PAMI",
                "01",
                "1234567890",
                "PRES-123",
                List.of()
        );

        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(gateway.validate(any())).thenReturn(new AdesfaGateway.GatewayResult(
                true, "{}", null, "REF-ADESFA", 200, false, "REST",
                new BigDecimal("300.00"), new BigDecimal("1200.00")
        ));
        when(validationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdesfaValidationResponse response = service.validateSale(saleId, request, "pharmacist");

        assertNotNull(response);
        assertEquals("APPROVED", response.status());
        assertEquals(new BigDecimal("300.00"), response.patientAmount());
        assertEquals(new BigDecimal("1200.00"), response.coverageAmount());

        verify(outboxService).enqueue(eq("ADESFA_VALIDATION"), any(), eq("ADESFA_VALIDATION_APPROVED"), any());
        verify(auditService).record(eq("pharmacist"), eq("ADESFA_VALIDATION_RECORDED"), any(), any(), any());
    }

    @Test
    void shouldHandleValidationError() throws Exception {
        UUID saleId = UUID.randomUUID();
        Sale sale = new Sale();
        sale.setId(saleId);
        sale.setTotalAmount(new BigDecimal("500.00"));

        AdesfaValidationRequest request = new AdesfaValidationRequest("OSDE", "01", "999", "P-1", List.of());

        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(gateway.validate(any())).thenReturn(new AdesfaGateway.GatewayResult(
                false, "{}", "Affiliate not found", null, 404, false, "REST",
                null, null
        ));
        when(validationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdesfaValidationResponse response = service.validateSale(saleId, request, "user");

        assertEquals("ERROR", response.status());
        verify(outboxService).enqueue(eq("ADESFA_VALIDATION"), any(), eq("ADESFA_VALIDATION_ERROR"), any());
    }

    @Test
    void shouldThrowNotFoundWhenSaleMissing() {
        UUID saleId = UUID.randomUUID();
        when(saleRepository.findById(saleId)).thenReturn(Optional.empty());

        AdesfaValidationRequest request = new AdesfaValidationRequest("X", "Y", "Z", "W", List.of());

        assertThrows(NotFoundException.class, () -> service.validateSale(saleId, request, "user"));
        verify(gateway, never()).validate(any());
    }

    @Test
    void shouldUseDefaultValidatorWhenNotProvided() throws Exception {
        UUID saleId = UUID.randomUUID();
        Sale sale = new Sale();
        sale.setId(saleId);
        sale.setTotalAmount(new BigDecimal("100.00"));

        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(properties.defaultValidatorCode()).thenReturn("DEFAULT_VALIDATOR");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(gateway.validate(any())).thenReturn(new AdesfaGateway.GatewayResult(
                true, "{}", null, null, 200, false, "MOCK", null, null
        ));
        when(validationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdesfaValidationRequest request = new AdesfaValidationRequest(null, "01", "1", "1", List.of());

        service.validateSale(saleId, request, "user");

        verify(gateway).validate(argThat(cmd -> cmd.validatorCode().equals("DEFAULT_VALIDATOR")));
    }
}
