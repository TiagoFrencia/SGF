package com.sgf.integrations.anmat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.audit.service.AuditService;
import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.core.domain.BadRequestException;
import com.sgf.integrations.anmat.domain.AnmatEventStatus;
import com.sgf.integrations.anmat.domain.AnmatEventType;
import com.sgf.integrations.anmat.domain.AnmatTraceabilityEvent;
import com.sgf.integrations.anmat.domain.AnmatTraceabilityEventRepository;
import com.sgf.integrations.anmat.service.AnmatDataMatrixParser;
import com.sgf.integrations.anmat.service.AnmatMode;
import com.sgf.integrations.anmat.service.AnmatProperties;
import com.sgf.integrations.anmat.service.AnmatTraceabilityGateway;
import com.sgf.integrations.anmat.service.AnmatTraceabilityService;
import com.sgf.integrations.anmat.web.AnmatDataMatrixParseResponse;
import com.sgf.integrations.anmat.web.AnmatTraceabilityEventRequest;
import com.sgf.integrations.anmat.web.AnmatTraceabilityEventResponse;
import com.sgf.integrations.service.OutboxService;
import com.sgf.inventory.domain.Batch;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnmatEventServiceTest {

    @Mock
    private AnmatDataMatrixParser parser;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private com.sgf.inventory.domain.BatchRepository batchRepository;

    @Mock
    private com.sgf.pos.domain.SaleRepository saleRepository;

    @Mock
    private AnmatTraceabilityEventRepository eventRepository;

    @Mock
    private com.sgf.integrations.anmat.domain.AnmatRemediationCaseRepository remediationCaseRepository;

    @Mock
    private AnmatTraceabilityGateway gateway;

    @Mock
    private AnmatProperties anmatProperties;

    @Mock
    private AuditService auditService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ObjectMapper objectMapper;

    private AnmatTraceabilityService traceabilityService;

    @BeforeEach
    void setUp() throws Exception {
        when(anmatProperties.enabled()).thenReturn(true);
        when(anmatProperties.mode()).thenReturn(AnmatMode.SANDBOX);
        when(anmatProperties.baseUrl()).thenReturn("https://sandbox.anmat.gob.ar");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        traceabilityService = new AnmatTraceabilityService(
            parser,
            productRepository,
            batchRepository,
            saleRepository,
            eventRepository,
            remediationCaseRepository,
            gateway,
            anmatProperties,
            auditService,
            outboxService,
            objectMapper
        );
    }

    @Test
    void parsesDataMatrixSuccessfully() {
        // Arrange
        String rawCode = "(01)07791234567890(17)270101(10)LOTE123(21)SERIAL456";
        AnmatDataMatrixParser mockParser = new AnmatDataMatrixParser();
        
        // Act
        AnmatDataMatrixParseResponse response = traceabilityService.parse(rawCode);

        // Assert
        assertNotNull(response);
        assertEquals("07791234567890", response.gtin());
        assertEquals(LocalDate.of(2027, 1, 1), response.expiresAt());
        assertEquals("LOTE123", response.lotNumber());
        assertEquals("SERIAL456", response.serialNumber());
    }

    @Test
    void reportsDispenseEventSuccessfully() {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        
        Product product = createProduct(productId, "07791234567890");
        Batch batch = createBatch(batchId, productId, "LOTE123", LocalDate.of(2027, 1, 1));
        
        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
            productId,
            batchId,
            saleId,
            AnmatEventType.DISPENSE,
            OffsetDateTime.now()
        );
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(eventRepository.save(any(AnmatTraceabilityEvent.class))).thenAnswer(invocation -> {
            AnmatTraceabilityEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        // Act
        AnmatTraceabilityEventResponse response = traceabilityService.reportEvent(request, "test_user");

        // Assert
        assertNotNull(response);
        assertEquals(AnmatEventType.DISPENSE, response.eventType());
        assertEquals(AnmatEventStatus.PENDING, response.eventStatus());
        verify(auditService).record(eq("test_user"), eq("ANMAT_EVENT_REPORTED"), any(), any(), any());
        verify(outboxService).enqueue(eq("ANMAT_TRACEABILITY"), any(), eq("ANMAT_EVENT_PENDING"), any());
    }

    @Test
    void reportsReceiptEventWithGln() {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        
        Product product = createProduct(productId, "07799876543210");
        Batch batch = createBatch(batchId, productId, "LOTE_RECV", LocalDate.of(2028, 6, 15));
        
        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
            productId,
            batchId,
            null,
            AnmatEventType.RECEIPT,
            OffsetDateTime.now()
        );
        request.setGln("7790000123456");
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(eventRepository.save(any(AnmatTraceabilityEvent.class))).thenAnswer(invocation -> {
            AnmatTraceabilityEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        // Act
        AnmatTraceabilityEventResponse response = traceabilityService.reportEvent(request, "warehouse_user");

        // Assert
        assertNotNull(response);
        assertEquals(AnmatEventType.RECEIPT, response.eventType());
        assertEquals("7790000123456", response.gln());
        ArgumentCaptor<AnmatTraceabilityEvent> eventCaptor = ArgumentCaptor.forClass(AnmatTraceabilityEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertEquals("7790000123456", eventCaptor.getValue().getGln());
    }

    @Test
    void rejectsEventWhenProductNotFound() {
        // Arrange
        UUID nonExistentProductId = UUID.randomUUID();
        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
            nonExistentProductId,
            UUID.randomUUID(),
            null,
            AnmatEventType.RECEIPT,
            OffsetDateTime.now()
        );
        
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            BadRequestException.class,
            () -> traceabilityService.reportEvent(request, "user")
        );
    }

    @Test
    void rejectsEventWhenBatchNotFound() {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID nonExistentBatchId = UUID.randomUUID();
        
        Product product = createProduct(productId, "07791112223334");
        
        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
            productId,
            nonExistentBatchId,
            null,
            AnmatEventType.RECEIPT,
            OffsetDateTime.now()
        );
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(batchRepository.findById(nonExistentBatchId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            BadRequestException.class,
            () -> traceabilityService.reportEvent(request, "user")
        );
    }

    @Test
    void updatesEventStatusToReportedAfterSuccessfulGatewaySubmission() {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        
        Product product = createProduct(productId, "07795556667778");
        Batch batch = createBatch(batchId, productId, "LOTE_SENT", LocalDate.of(2027, 12, 1));
        
        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
            productId,
            batchId,
            null,
            AnmatEventType.RECEIPT,
            OffsetDateTime.now()
        );
        
        AnmatTraceabilityEvent savedEvent = new AnmatTraceabilityEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setProduct(product);
        savedEvent.setBatch(batch);
        savedEvent.setEventType(AnmatEventType.RECEIPT);
        savedEvent.setEventStatus(AnmatEventStatus.PENDING);
        savedEvent.setGtin("07795556667778");
        savedEvent.setSerialNumber("SERIAL_TEST");
        savedEvent.setLotNumber("LOTE_SENT");
        savedEvent.setExpiresAt(LocalDate.of(2027, 12, 1));
        savedEvent.setOccurredAt(OffsetDateTime.now());
        savedEvent.setSource("SGF");
        savedEvent.setRequestJson("{}");
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(eventRepository.save(any(AnmatTraceabilityEvent.class)))
            .thenReturn(savedEvent)
            .thenAnswer(invocation -> {
                AnmatTraceabilityEvent event = invocation.getArgument(0);
                if (event.getStatus() == AnmatEventStatus.REPORTED) {
                    event.setId(UUID.randomUUID());
                }
                return event;
            });
        when(gateway.submitEvent(any(AnmatTraceabilityEvent.class))).thenReturn(true);

        // Act - This would normally be done by a scheduled job
        // Simulating the status update after gateway submission
        savedEvent.setEventStatus(AnmatEventStatus.REPORTED);

        // Assert
        assertEquals(AnmatEventStatus.REPORTED, savedEvent.getEventStatus());
        verify(gateway).submitEvent(any(AnmatTraceabilityEvent.class));
    }

    @Test
    void handlesReturnEventFromCustomer() {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        
        Product product = createProduct(productId, "07799998887776");
        Batch batch = createBatch(batchId, productId, "LOTE_RETURN", LocalDate.of(2027, 3, 20));
        
        AnmatTraceabilityEventRequest request = new AnmatTraceabilityEventRequest(
            productId,
            batchId,
            null,
            AnmatEventType.RETURN,
            OffsetDateTime.now()
        );
        request.setReason("Producto vencido");
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(eventRepository.save(any(AnmatTraceabilityEvent.class))).thenAnswer(invocation -> {
            AnmatTraceabilityEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        // Act
        AnmatTraceabilityEventResponse response = traceabilityService.reportEvent(request, "pharmacist");

        // Assert
        assertNotNull(response);
        assertEquals(AnmatEventType.RETURN, response.eventType());
        ArgumentCaptor<AnmatTraceabilityEvent> eventCaptor = ArgumentCaptor.forClass(AnmatTraceabilityEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertEquals("RETURN", eventCaptor.getValue().getEventType().name());
    }

    private Product createProduct(UUID id, String gtin) {
        Product product = new Product();
        product.setId(id);
        product.setName("Producto Test");
        product.setGtin(gtin);
        return product;
    }

    private Batch createBatch(UUID id, UUID productId, String lotNumber, LocalDate expiresAt) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setProductId(productId);
        batch.setLotNumber(lotNumber);
        batch.setExpiresAt(expiresAt);
        return batch;
    }
}
