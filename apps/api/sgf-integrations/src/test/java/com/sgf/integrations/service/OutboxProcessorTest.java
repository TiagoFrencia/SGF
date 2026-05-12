package com.sgf.integrations.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.integrations.afip.service.AfipProperties;
import com.sgf.integrations.afip.service.AfipService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock OutboxEventRepository repository;
    @Mock AfipService afipService;
    @Mock AfipProperties afipProperties;

    @Test
    void saleCompletedIsCompletedWithoutAfipWhenAutoInvoiceDisabled() {
        OutboxEvent event = saleCompletedEvent();
        when(repository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(afipProperties.enabled()).thenReturn(true);
        when(afipProperties.autoInvoiceEnabled()).thenReturn(false);

        new OutboxProcessor(repository, afipService, afipProperties, new ObjectMapper()).processPendingEvents();

        assertEquals("COMPLETED", event.getStatus());
        verify(afipService, never()).authorizeSaleInvoice(any(), any(), any());
        verify(repository).save(event);
    }

    @Test
    void saleCompletedCallsAfipWhenAutoInvoiceEnabled() {
        OutboxEvent event = saleCompletedEvent();
        when(repository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(afipProperties.enabled()).thenReturn(true);
        when(afipProperties.autoInvoiceEnabled()).thenReturn(true);
        when(afipProperties.pointOfSale()).thenReturn(1);

        new OutboxProcessor(repository, afipService, afipProperties, new ObjectMapper()).processPendingEvents();

        assertEquals("COMPLETED", event.getStatus());
        verify(afipService).authorizeSaleInvoice(eq(event.getAggregateId()), any(), eq("outbox-processor"));
    }

    private OutboxEvent saleCompletedEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("SALE");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("SALE_COMPLETED");
        event.setStatus("PENDING");
        event.setPayloadJson("""
                {
                  "customerDocument": "30111222",
                  "items": [
                    {"productId": "00000000-0000-0000-0000-000000000001", "requiresTraceability": true, "batchId": ""}
                  ]
                }
                """);
        return event;
    }
}
