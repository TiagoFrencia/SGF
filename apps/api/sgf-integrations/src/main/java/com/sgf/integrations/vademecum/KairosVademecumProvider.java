package com.sgf.integrations.vademecum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class KairosVademecumProvider implements VademecumProvider {

    private final KairosConnector connector;

    public KairosVademecumProvider(KairosConnector connector) {
        this.connector = connector;
    }

    @Override
    public String providerCode() {
        return "KAIROS";
    }

    @Override
    public List<VademecumProduct> search(String searchData) {
        return connector.findByGtin(searchData)
                .map(product -> List.of(toProduct(product)))
                .orElseGet(List::of);
    }

    @Override
    public Optional<java.time.LocalDate> currentVigencia() {
        return Optional.empty();
    }

    private VademecumProduct toProduct(KairosConnector.KairosProduct product) {
        return new VademecumProduct(
                product.gtin(),
                product.gtin(),
                null,
                null,
                product.commercialName(),
                null,
                product.laboratory(),
                null,
                product.activeIngredient(),
                null,
                null,
                product.pharmaceuticalForm(),
                null,
                decimal(product.retailPrice()),
                decimal(product.pamiPrice()),
                null,
                null,
                null);
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }
}
