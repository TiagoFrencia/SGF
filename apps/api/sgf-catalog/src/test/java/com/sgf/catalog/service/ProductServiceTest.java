package com.sgf.catalog.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.catalog.web.CreateProductRequest;
import com.sgf.catalog.web.ProductResponse;
import com.sgf.core.domain.ConflictException;
import java.util.Optional;
import java.util.UUID;
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
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ProductService service;

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
}
