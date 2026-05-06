package com.sgf.modules.integrations.anmat.web;

import com.sgf.modules.integrations.anmat.service.AnmatDataMatrix;
import java.time.LocalDate;

public record AnmatDataMatrixParseResponse(
        String gtin,
        LocalDate expiresAt,
        String lotNumber,
        String serialNumber
) {
    public static AnmatDataMatrixParseResponse from(AnmatDataMatrix value) {
        return new AnmatDataMatrixParseResponse(value.gtin(), value.expiresAt(), value.lotNumber(), value.serialNumber());
    }
}

