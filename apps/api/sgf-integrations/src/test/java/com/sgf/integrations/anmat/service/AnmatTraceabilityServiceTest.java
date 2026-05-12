package com.sgf.integrations.anmat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.audit.service.AuditService;
import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.core.domain.BadRequestException;
import com.sgf.core.domain.ConflictException;
import com.sgf.integrations.anmat.domain.AnmatEventStatus;
import com.sgf.integrations.anmat.domain.AnmatEventType;
import com.sgf.integrations.anmat.domain.AnmatRemediationCaseRepository;
import com.sgf.integrations.anmat.domain.AnmatTraceabilityEvent;
import com.sgf.integrations.anmat.domain.AnmatTraceabilityEventRepository;
import com.sgf.integrations.anmat.web.AnmatTraceabilityEventRequest;
import com.sgf.integrations.anmat.web.AnmatTraceabilityEventResponse;
import com.sgf.integrations.service.OutboxService;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnmatTraceabilityServiceTest {

    @Mock private AnmatDataMatrixParser parser;
    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private AnmatTraceabilityEventRepository eventRepository;
    @Mock private AnmatRemediationCaseRepository remediationCaseRepository;
    @Mock private AnmatTraceabilityGateway gateway;
    @Mock private AnmatProperties properties;
    @Mock private AuditService auditService;
    @Mock private OutboxService outboxService;
    @Mock private ObjectMapper objectMapper;

    private AnmatTraceabilityService service;

    @BeforeEach
    void setUp() {
        service = new AnmatTraceabilityService(
                parser, productRepository, batchRepository, saleRepository,
                eventRepository, remediationCaseRepository, gateway,
                properties, auditService, outboxService, objectMapper
        );
    }

    @Test
    void shouldReportReceiptSuccessfully() throws Exception {
        String gtin = "7791234567890";
        String lot = "L123";
        String serial = "S123";
        String dm = "(01)" + gtin + "(17)300101(10)" + lot + "(21)" + serial;

        AnmatDataMatrix parsed = new AnmatDataMatrix(gtin, LocalDate.now().plusYears(1), lot, serial);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setRequiresTraceability(true);
        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());

        when(parser.parse(dm)).thenReturn(parsed);
        when(productRepository.findByGtin(gtin)).thenReturn(Optional.of(product));
        when(batchRepository.findByProductIdAndLotNumber(product.getId(), lot)).thenReturn(Optional.of(batch));
        when(gateway.report(any())).thenReturn(new AnmatTraceabilityGateway.GatewayResult(true, "{}", null, "REF-1", 200, false, "REST"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
                AnmatEventType.RECEIPT, dm, "GLN-1", null, OffsetDateTime.now(), "UI");

        AnmatTraceabilityEventResponse response = service.report(request, "admin");

        assertNotNull(response);
        assertEquals("REPORTED", response.eventStatus());
        verify(outboxService).enqueue(eq("ANMAT_EVENT"), any(), eq("ANMAT_TRACEABILITY_REPORTED"), any());
        verify(auditService).record(eq("admin"), eq("ANMAT_TRACEABILITY_REPORTED"), any(), any(), any());
    }

    @Test
    void shouldThrowWhenProductIsNotTraceable() {
        String dm = "DM-123";
        AnmatDataMatrix parsed = new AnmatDataMatrix("GTIN", LocalDate.now(), "L", "S");
        Product product = new Product();
        product.setRequiresTraceability(false);

        when(parser.parse(dm)).thenReturn(parsed);
        when(productRepository.findByGtin("GTIN")).thenReturn(Optional.of(product));

        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
                AnmatEventType.RECEIPT, dm, "G", null, OffsetDateTime.now(), "S");

        assertThrows(BadRequestException.class, () -> service.report(request, "user"));
    }

    @Test
    void shouldThrowConflictWhenDuplicateEventExists() {
        String dm = "DM-123";
        AnmatDataMatrix parsed = new AnmatDataMatrix("GTIN", LocalDate.now(), "LOT", "SERIAL");
        Product product = new Product();
        product.setRequiresTraceability(true);

        when(parser.parse(dm)).thenReturn(parsed);
        when(productRepository.findByGtin("GTIN")).thenReturn(Optional.of(product));
        when(eventRepository.findByEventTypeAndGtinAndSerialNumber(any(), any(), any()))
                .thenReturn(Optional.of(new AnmatTraceabilityEvent()));

        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
                AnmatEventType.RECEIPT, dm, "G", null, OffsetDateTime.now(), "S");

        assertThrows(ConflictException.class, () -> service.report(request, "user"));
    }

    @Test
    void shouldDetectDispenseWithoutReceiptAsInconsistency() {
        AnmatTraceabilityEvent dispense = new AnmatTraceabilityEvent();
        dispense.setEventType(AnmatEventType.DISPENSE);
        dispense.setGtin("G1");
        dispense.setSerialNumber("S1");
        dispense.setOccurredAt(OffsetDateTime.now());

        when(eventRepository.findAll()).thenReturn(List.of(dispense));

        var inconsistencies = service.inconsistencies();

        assertEquals(2, inconsistencies.size());
    }

    @Test
    void shouldMarkGatewayFailureAsFailed() throws Exception {
        String dm = "DM-FAIL";
        AnmatDataMatrix parsed = new AnmatDataMatrix("G", LocalDate.now(), "L", "S");
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setRequiresTraceability(true);
        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());

        when(parser.parse(dm)).thenReturn(parsed);
        when(productRepository.findByGtin("G")).thenReturn(Optional.of(product));
        when(batchRepository.findByProductIdAndLotNumber(product.getId(), "L")).thenReturn(Optional.of(batch));
        when(gateway.report(any())).thenReturn(new AnmatTraceabilityGateway.GatewayResult(false, "{}", "Server Error", null, 500, true, "REST"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
                AnmatEventType.RECEIPT, dm, "GLN", null, OffsetDateTime.now(), "S");

        AnmatTraceabilityEventResponse response = service.report(request, "admin");

        assertEquals("FAILED", response.eventStatus());
        verify(outboxService).enqueue(eq("ANMAT_EVENT"), any(), eq("ANMAT_TRACEABILITY_FAILED"), any());
    }

    @Test
    void shouldThrowWhenDispenseDoesNotReferenceSale() {
        String dm = "DM-DISPENSE";
        AnmatDataMatrix parsed = new AnmatDataMatrix("G", LocalDate.now(), "L", "S");
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setRequiresTraceability(true);
        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());

        when(parser.parse(dm)).thenReturn(parsed);
        when(productRepository.findByGtin("G")).thenReturn(Optional.of(product));
        when(batchRepository.findByProductIdAndLotNumber(product.getId(), "L")).thenReturn(Optional.of(batch));

        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
                AnmatEventType.DISPENSE, dm, "GLN", null, OffsetDateTime.now(), "S");

        assertThrows(BadRequestException.class, () -> service.report(request, "user"));
    }
}
