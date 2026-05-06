package com.sgf.modules.integrations.anmat.web;

import jakarta.validation.constraints.NotBlank;

public record AnmatDataMatrixParseRequest(
        @NotBlank String dataMatrix
) {
}

