package com.sgf.audit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgf.audit.domain.AuditEventRepository;
import com.sgf.core.context.TenantContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    AuditEventRepository repository;

    @InjectMocks
    AuditService service;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void recordSavesEventWithTenantFromContext() {
        TenantContext.setTenantId("tenant-test-001");
        when(repository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());
        service.record("admin", "LOGIN", "AUTH", UUID.randomUUID(), "{}");
        verify(repository).save(argThat(event -> "tenant-test-001".equals(event.getTenantId())));
    }

    @Test
    void recordFallsBackToDefaultTenantWhenContextMissing() {
        when(repository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());
        service.record("admin", "LOGIN", "AUTH", UUID.randomUUID(), "{}");
        verify(repository).save(argThat(event -> AuditService.DEFAULT_TENANT_ID.equals(event.getTenantId())));
    }
}
