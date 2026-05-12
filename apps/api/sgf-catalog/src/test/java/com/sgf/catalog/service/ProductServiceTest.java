package com.sgf.catalog.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductPriceSnapshot;
import com.sgf.catalog.domain.ProductPriceSnapshotRepository;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.catalog.web.CreateProductRequest;
import com.sgf.catalog.web.ProductResponse;
import com.sgf.core.domain.ConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;

    @Mock
    ProductPriceSnapshotRepository priceSnapshotRepository;

    @Mock
    ProductPricingService productPricingService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ProductService service;

    @BeforeEach
    void setUp() {
        lenient().when(productPricingService.latestPrice(any())).thenReturn(Optional.empty());
    }

    @Test
    void createProductSucceeds() {
        CreateProductRequest request = new CreateProductRequest("07791234567890", "SKU1", "Name", "Brand", "AI", true, false, "ALTO_RIESGO", "Desc", "500", "Form", 1);
        when(repository.existsByGtinOrSku("07791234567890", "SKU1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> {
            Product p = (Product) i.getArguments()[0];
            p.setId(UUID.randomUUID());
            return p;
        });

        ProductResponse result = service.create(request, "admin");
        assertNotNull(result);
    }

    @Test
    void createProductFailsOnDuplicateGtin() {
        CreateProductRequest request = new CreateProductRequest("07791234567890", "SKU1", "Name", "Brand", "AI", true, false, "ALTO_RIESGO", "Desc", "500", "Form", 1);
        when(repository.existsByGtinOrSku("07791234567890", "SKU1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request, "admin"));
    }

    @Test
    void shouldFindProduct_WhenExists() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(product));

        Product result = service.findEntity(id);
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void shouldThrowNotFound_WhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(com.sgf.core.domain.NotFoundException.class, () -> service.findEntity(id));
    }

    @Test
    void shouldPublishEvent_WhenProductCreated() {
        CreateProductRequest request = new CreateProductRequest("123", "SKU", "N", "B", "C", true, false, "LOW", "D", "P", "F", 1);
        when(repository.existsByGtinOrSku(any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        service.create(request, "admin");

        verify(eventPublisher).publishEvent(any(com.sgf.core.event.ProductCreatedEvent.class));
    }

    @Test
    void importFromVademecumPrefersGtinForDeduplication() {
        Product existing = new Product();
        existing.setId(UUID.randomUUID());
        existing.setGtin("07798006301810");
        existing.setSku("OLD");
        when(repository.findByGtin("07798006301810")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(priceSnapshotRepository.findByProductAndSourceAndSourceRecordKeyAndEffectiveDate(
                any(), any(), any(), any())).thenReturn(Optional.empty());
        when(priceSnapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductResponse result = service.importFromVademecum(publicImportCommand(
                "07798006301810", "554742", "IBUPROFENO FECOFAR", "susp.oral x 90 ml"));

        assertEquals(existing.getId(), result.id());
        assertEquals("554742", result.troquel());
        verify(repository).findByGtin("07798006301810");
    }

    @Test
    void importFromVademecumFallsBackToNamePresentationAndLaboratory() {
        Product existing = new Product();
        existing.setId(UUID.randomUUID());
        existing.setGtin("00000000000001");
        existing.setSku("OLD");
        when(repository.findByCommercialNamePresentationAndLaboratory(
                "IBUPROFENO FECOFAR", "susp.oral x 90 ml", "Fecofar"))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductResponse result = service.importFromVademecum(publicImportCommand(
                null, null, "IBUPROFENO FECOFAR", "susp.oral x 90 ml"));

        assertEquals(existing.getId(), result.id());
        verify(repository).findByCommercialNamePresentationAndLaboratory(
                "IBUPROFENO FECOFAR", "susp.oral x 90 ml", "Fecofar");
    }

    @Test
    void importFromVademecumStoresVersionedPriceSnapshot() {
        when(repository.save(any())).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(priceSnapshotRepository.findByProductAndSourceAndSourceRecordKeyAndEffectiveDate(
                any(), any(), any(), any())).thenReturn(Optional.empty());

        service.importFromVademecum(publicImportCommand(
                "07798006301810", "554742", "IBUPROFENO FECOFAR", "susp.oral x 90 ml"));

        verify(priceSnapshotRepository).save(any(ProductPriceSnapshot.class));
    }

    @Test
    void shouldResolveLatestPriceQuote() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        ProductPriceSnapshot older = priceSnapshot(product, "2026-04-01", "100.00");
        ProductPriceSnapshot latest = priceSnapshot(product, "2026-05-01", "125.50");
        when(priceSnapshotRepository.findTopByProductOrderByEffectiveDateDescCreatedAtDesc(product))
                .thenReturn(Optional.of(latest));

        ProductPricingService pricingService = new ProductPricingService(priceSnapshotRepository);

        ProductPriceQuote quote = pricingService.latestPrice(product).orElseThrow();

        assertEquals(new BigDecimal("125.50"), quote.retailPrice());
        assertEquals(java.time.LocalDate.of(2026, 5, 1), quote.effectiveDate());
    }

    private ProductPriceSnapshot priceSnapshot(Product product, String effectiveDate, String retailPrice) {
        ProductPriceSnapshot snapshot = new ProductPriceSnapshot();
        snapshot.setProduct(product);
        snapshot.setSource("CNPM_MSAL");
        snapshot.setSourceRecordKey("39131-" + effectiveDate);
        snapshot.setEffectiveDate(LocalDate.parse(effectiveDate));
        snapshot.setRetailPrice(new BigDecimal(retailPrice));
        return snapshot;
    }

    private VademecumProductImportCommand publicImportCommand(
            String gtin, String troquel, String name, String presentation) {
        return new VademecumProductImportCommand(
                "CNPM_MSAL",
                "39131",
                gtin,
                troquel,
                "7798006301810",
                name,
                presentation,
                "Fecofar",
                "00092",
                "ibuprofeno",
                "284601000221102",
                "Venta Libre",
                "Jarabe/Suspensión oral/Polvo para uso oral",
                1,
                new BigDecimal("615"),
                new BigDecimal("369.00"),
                7,
                "40% de descuento",
                LocalDate.of(2026, 4, 30));
    }
}
