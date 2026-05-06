package com.sgf.integrations.anmat.web;

import com.sgf.integrations.anmat.domain.AnmatRemediationCase;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnmatRemediationCaseResponse(
        UUID id,
        String gtin,
        String serialNumber,
        String lotNumber,
        String issueCode,
        String severity,
        String recommendation,
        String status,
        String notes,
        String lastReason,
        String assignedTo,
        String lastActionBy,
        OffsetDateTime resolvedAt,
        OffsetDateTime updatedAt
) {
    public static AnmatRemediationCaseResponse from(AnmatRemediationCase value) {
        return new AnmatRemediationCaseResponse(
                value.getId(),
                value.getGtin(),
                value.getSerialNumber(),
                value.getLotNumber(),
                value.getIssueCode(),
                value.getSeverity(),
                value.getRecommendation(),
                value.getStatus().name(),
                value.getNotes(),
                value.getLastReason(),
                value.getAssignedTo(),
                value.getLastActionBy(),
                value.getResolvedAt(),
                value.getUpdatedAt()
        );
    }
}
