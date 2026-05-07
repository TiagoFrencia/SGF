package com.sgf.integrations.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    OutboxEventRepository repository;

    @InjectMocks
    OutboxService service;

    @Test
    void enqueueSavesEvent() {
        service.enqueue("TYPE", UUID.randomUUID(), "EVENT", "{}");
        verify(repository).save(any());
    }
}
