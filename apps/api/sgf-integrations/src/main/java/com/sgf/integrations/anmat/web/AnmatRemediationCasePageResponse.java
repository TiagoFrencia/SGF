package com.sgf.integrations.anmat.web;

import java.util.List;

public record AnmatRemediationCasePageResponse(
        List<AnmatRemediationCaseResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sortBy,
        String sortDirection
) {
}
