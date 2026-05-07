package com.sgf.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleRepository;
import com.sgf.pos.service.SalesService;
import com.sgf.pos.web.SaleRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class FullFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Test
    void testCompleteSaleFlow() {
        // 1. Setup Product
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setGtin("7791234567890");
        product.setCommercialName("IBUPROFENO 600MG");
        product.setRetailPrice(new BigDecimal("1500.00"));
        productRepository.save(product);

        // 2. Execute Sale via REST
        SaleRequest.SaleItemRequest item = new SaleRequest.SaleItemRequest(product.getId(), 2, product.getRetailPrice());
        SaleRequest request = new SaleRequest(List.of(item), "CASH", null);
        
        ResponseEntity<Sale> response = restTemplate.postForEntity("/api/sales", request, Sale.class);
        
        // 3. Verify
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Sale sale = response.getBody();
        assertThat(sale).isNotNull();
        assertThat(sale.getTotalAmount()).isEqualByComparingTo("3000.00");
        
        // 4. Verify in DB
        assertThat(saleRepository.findById(sale.getId())).isPresent();
    }
}
