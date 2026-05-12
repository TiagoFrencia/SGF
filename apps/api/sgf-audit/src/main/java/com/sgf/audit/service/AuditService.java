package com.sgf.audit.service;

import com.sgf.audit.domain.AuditEvent;
import com.sgf.audit.domain.AuditEventRepository;
import com.sgf.audit.web.AuditEventResponse;
import com.sgf.core.context.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @org.springframework.transaction.annotation.Transactional
    public void record(String actorUsername, String eventType, String aggregateType, UUID aggregateId, String detailsJson) {
        record(actorUsername, eventType, aggregateType, aggregateId, detailsJson, resolveTenantId());
    }

    @org.springframework.transaction.annotation.Transactional
    public void record(String actorUsername, String eventType, String aggregateType, UUID aggregateId, String detailsJson, String tenantId) {
        String lastHash = auditEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1))
                .stream().findFirst().map(AuditEvent::getHash).orElse("0".repeat(64));

        AuditEvent event = new AuditEvent();
        event.setActorUsername(actorUsername);
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setDetailsJson(detailsJson);
        event.setTenantId(tenantId == null || tenantId.isBlank() ? DEFAULT_TENANT_ID : tenantId);

        event.setPreviousHash(lastHash);
        
        String dataToHash = lastHash
                + nullSafe(actorUsername)
                + nullSafe(eventType)
                + (aggregateId != null ? aggregateId.toString() : "")
                + nullSafe(detailsJson);
        event.setHash(calculateHash(dataToHash));
        
        auditEventRepository.save(event);
    }

    private String resolveTenantId() {
        String tenantId = TenantContext.getTenantId();
        return tenantId == null || tenantId.isBlank() ? DEFAULT_TENANT_ID : tenantId;
    }

    private String calculateHash(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash calculation failed", e);
        }
    }

    public List<AuditEventResponse> latest(int limit) {
        return auditEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AuditChainVerification verifyChain(int limit) {
        List<AuditEvent> ordered = auditEventRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, limit));
        if (ordered.isEmpty()) {
            return new AuditChainVerification(true, 0, null, "Empty chain");
        }

        String expectedPrevious = "0".repeat(64);
        int verified = 0;
        for (AuditEvent event : ordered) {
            String prev = event.getPreviousHash();
            if (prev == null || !prev.equals(expectedPrevious)) {
                return new AuditChainVerification(
                        false,
                        verified,
                        event.getId(),
                        "Broken previous hash link"
                );
            }

            String dataToHash = expectedPrevious
                    + nullSafe(event.getActorUsername())
                    + nullSafe(event.getEventType())
                    + (event.getAggregateId() != null ? event.getAggregateId().toString() : "")
                    + nullSafe(event.getDetailsJson());

            String expectedHash = calculateHash(dataToHash);
            if (event.getHash() == null || !event.getHash().equals(expectedHash)) {
                return new AuditChainVerification(
                        false,
                        verified,
                        event.getId(),
                        "Hash mismatch"
                );
            }

            expectedPrevious = event.getHash();
            verified++;
        }

        return new AuditChainVerification(true, verified, null, "OK");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }


    public record AuditChainVerification(
            boolean valid,
            int verifiedEvents,
            UUID brokenEventId,
            String message
    ) {}
}
