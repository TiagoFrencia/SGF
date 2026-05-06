package com.sgf.integrations.anmat.service;

import java.time.LocalDate;

public record AnmatDataMatrix(
        String gtin,
        LocalDate expiresAt,
        String lotNumber,
        String serialNumber
) {
}

