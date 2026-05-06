package com.sgf.integrations.afip.wsaa;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class LoginTicketRequestBuilder {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private LoginTicketRequestBuilder() {
    }

    public static String build(String service, OffsetDateTime now) {
        OffsetDateTime generation = now.minusMinutes(5).withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime expiration = now.plusMinutes(10).withOffsetSameInstant(ZoneOffset.UTC);
        long uniqueId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        return """
                <loginTicketRequest version="1.0">
                  <header>
                    <uniqueId>%d</uniqueId>
                    <generationTime>%s</generationTime>
                    <expirationTime>%s</expirationTime>
                  </header>
                  <service>%s</service>
                </loginTicketRequest>
                """.formatted(
                uniqueId,
                FORMATTER.format(generation),
                FORMATTER.format(expiration),
                service
        );
    }
}

