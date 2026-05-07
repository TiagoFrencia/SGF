package com.sgf.audit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgf.audit.domain.AuditEventRepository;
import java.util.List;
import java.util.UUID;
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

    @Test
    void recordSavesEvent() {
        when(repository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());
        service.record("admin", "LOGIN", "AUTH", UUID.randomUUID(), "{}");
        verify(repository).save(any());
    }
}
