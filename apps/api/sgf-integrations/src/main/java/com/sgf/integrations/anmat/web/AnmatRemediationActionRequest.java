package com.sgf.integrations.anmat.web;

import com.sgf.integrations.anmat.domain.AnmatRemediationStatus;
import jakarta.validation.constraints.NotNull;

public record AnmatRemediationActionRequest(
        @NotNull AnmatRemediationStatus status,
        String notes,
        String assignedTo,
        String reason
) {
}
