package com.sgf.integrations.vademecum;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PublicMsalVademecumProviderTest {

    private final PublicMsalVademecumProvider provider = new PublicMsalVademecumProvider(new ObjectMapper());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsPublicApiFieldsFromAnonymizedFixture() throws Exception {
        var node = objectMapper.readTree("""
                {
                  "NOMBRE": "IBUPROFENO FECOFAR",
                  "PRESENTACION": "susp.oral x 90 ml",
                  "LABORATORIO": "Fecofar",
                  "FECHA": "30/04/2026",
                  "PRECIO": 615,
                  "DROGA": "ibuprofeno",
                  "TIPO_DE_VENTA": "Venta Libre",
                  "D_PAMI": "40% de descuento",
                  "C_PAMI": 7,
                  "C_LABORATORIO": "00092",
                  "C_BARRA": "7798006301810",
                  "SNOMED": "284601000221102",
                  "GTIN1": "07798006301810",
                  "TROQUEL": "554742",
                  "FORMA": "Jarabe/Suspension oral",
                  "UNIDADES": 1,
                  "CLAVE": 39131,
                  "PRECIOPAMI": 0
                }
                """);

        var product = provider.parseProduct(node);

        assertThat(product.sourceRecordKey()).isEqualTo("39131");
        assertThat(product.gtin()).isEqualTo("07798006301810");
        assertThat(product.troquel()).isEqualTo("554742");
        assertThat(product.snomedCode()).isEqualTo("284601000221102");
        assertThat(product.retailPrice()).isEqualByComparingTo("615");
        assertThat(product.pamiAffiliatePrice()).isEqualByComparingTo("369.00");
        assertThat(product.effectiveDate()).isEqualTo("2026-04-30");
    }

    @Test
    void calculatesPamiAffiliatePriceUsingExplicitPamiReferencePriceWhenAvailable() {
        BigDecimal price = provider.calculatePamiAffiliatePrice(
                new BigDecimal("1000"), new BigDecimal("800"), 2);

        assertThat(price).isEqualByComparingTo("400.00");
    }

    @Test
    void returnsNullPamiAffiliatePriceWhenNoDiscountInfoExists() {
        BigDecimal price = provider.calculatePamiAffiliatePrice(new BigDecimal("1000"), BigDecimal.ZERO, 0);

        assertThat(price).isNull();
    }

    @Test
    void keepsFallbackFieldsWhenSnomedGtinOrTroquelAreMissing() throws Exception {
        var node = objectMapper.readTree("""
                {
                  "NOMBRE": "PRODUCTO SIN CODIGOS",
                  "PRESENTACION": "comp.x 10",
                  "LABORATORIO": "Lab",
                  "FECHA": "01/05/2026",
                  "PRECIO": 1000,
                  "DROGA": "droga",
                  "C_PAMI": 0,
                  "CLAVE": 999
                }
                """);

        var product = provider.parseProduct(node);

        assertThat(product.gtin()).isNull();
        assertThat(product.troquel()).isNull();
        assertThat(product.snomedCode()).isNull();
        assertThat(product.commercialName()).isEqualTo("PRODUCTO SIN CODIGOS");
        assertThat(product.presentation()).isEqualTo("comp.x 10");
    }
}
