package com.sgf.core.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import com.sgf.core.domain.BaseEntity;

/**
 * Registro de auditoría regulatoria inmutable.
 * Almacena eventos de AFIP, ANMAT, ADESFA con hash para integridad.
 */
@Entity
@Table(name = "regulatory_audit_logs")
public class RegulatoryAuditLog extends BaseEntity {

    @Column(nullable = false)
    private String actorUsername;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditSourceType sourceType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, unique = true)
    private String integrityHash;

    // Getters and Setters
    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public AuditSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(AuditSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIntegrityHash() {
        return integrityHash;
    }

    public void setIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }

    public enum AuditSourceType {
        AFIP,
        ANMAT,
        ADESFA,
        RENAPDIS,
        PAMI,
        SYSTEM
    }
}
