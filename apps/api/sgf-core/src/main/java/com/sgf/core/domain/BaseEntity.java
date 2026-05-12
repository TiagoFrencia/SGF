package com.sgf.core.domain;

import com.sgf.core.context.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.OffsetDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";

    @Id
    private UUID id;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;
 
    @Column(nullable = false)
    private String tenantId;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = DEFAULT_TENANT_ID;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
 
    public String getTenantId() {
        return tenantId;
    }
 
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
