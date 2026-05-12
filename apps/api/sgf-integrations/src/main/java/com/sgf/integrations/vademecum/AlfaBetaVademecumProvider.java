package com.sgf.integrations.vademecum;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AlfaBetaVademecumProvider implements VademecumProvider {

    private final AlfaBetaConnector connector;

    public AlfaBetaVademecumProvider(AlfaBetaConnector connector) {
        this.connector = connector;
    }

    @Override
    public String providerCode() {
        return "ALFABETA";
    }

    @Override
    public List<VademecumProduct> search(String searchData) {
        return connector.findByActiveIngredient(searchData, 50).stream()
                .map(product -> new VademecumProduct(
                        product.gtin(),
                        product.gtin(),
                        null,
                        null,
                        product.commercialName(),
                        product.presentation(),
                        product.laboratory(),
                        null,
                        product.activeIngredient(),
                        null,
                        product.requiresPrescription() ? "Venta Bajo Receta" : null,
                        product.pharmaceuticalForm(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
                .toList();
    }

    @Override
    public Optional<java.time.LocalDate> currentVigencia() {
        return Optional.empty();
    }
}
