package com.sgf.modules.audit.service;

import com.sgf.modules.audit.domain.AuditEvent;
import com.sgf.modules.audit.domain.AuditEventRepository;
import com.sgf.modules.audit.web.AuditEventResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(String actorUsername, String eventType, String aggregateType, UUID aggregateId, String detailsJson) {
        AuditEvent event = new AuditEvent();
        event.setActorUsername(actorUsername);
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setDetailsJson(detailsJson);
        auditEventRepository.save(event);
    }

    public List<AuditEventResponse> latest(int limit) {
        return auditEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(AuditEventResponse::from)
                .toList();
    }
}

