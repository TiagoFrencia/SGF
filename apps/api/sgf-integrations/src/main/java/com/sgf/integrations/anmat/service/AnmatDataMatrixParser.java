package com.sgf.integrations.anmat.service;

import com.sgf.core.domain.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AnmatDataMatrixParser {

    private static final Pattern GS1_PATTERN = Pattern.compile("\\(01\\)(\\d{14})\\(17\\)(\\d{6})\\(10\\)([^\\(]+)\\(21\\)(.+)");
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    public AnmatDataMatrix parse(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new BadRequestException("DataMatrix code is required");
        }
        Matcher matcher = GS1_PATTERN.matcher(rawCode.trim());
        if (!matcher.matches()) {
            throw new BadRequestException("Unsupported DataMatrix format. Expected (01)GTIN(17)VENC(10)LOTE(21)SERIAL");
        }
        return new AnmatDataMatrix(
                matcher.group(1),
                LocalDate.parse(matcher.group(2), YYMMDD),
                matcher.group(3).trim(),
                matcher.group(4).trim()
        );
    }
}
