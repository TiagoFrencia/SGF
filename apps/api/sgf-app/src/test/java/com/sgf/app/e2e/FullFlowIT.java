package com.sgf.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.sgf.app.support.PostgresIntegrationTestSupport;
import com.sgf.app.support.IntegrationFixtures;
import com.sgf.catalog.domain.Product;
import com.sgf.catalog.domain.ProductRepository;
import com.sgf.inventory.domain.Batch;
import com.sgf.inventory.domain.BatchRepository;
import com.sgf.modules.auth.web.AuthResponse;
import com.sgf.pos.domain.SaleRepository;
import com.sgf.pos.web.CreateSaleRequest;
import com.sgf.pos.web.SaleResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
public class FullFlowIT extends PostgresIntegrationTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Test
    void testCompleteSaleFlow() {
        // 1. Setup Product
        Product product = IntegrationFixtures.validProduct("7791234567890", "IBU-600", "IBUPROFENO 600MG");
        productRepository.save(product);
        Batch batch = IntegrationFixtures.validBatch(product, "FLOW-LOT-001", 10, new BigDecimal("950.00"));
        batchRepository.save(batch);

        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
                "/auth/login",
                new HttpEntity<>(java.util.Map.of("username", "admin", "password", "admin1234")),
                AuthResponse.class);
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authResponse.getBody()).isNotNull();
        String token = authResponse.getBody().accessToken();

        // 2. Execute Sale via REST
        CreateSaleRequest.SaleLineRequest item = new CreateSaleRequest.SaleLineRequest(product.getId(), 2, new BigDecimal("1500.00"));
        CreateSaleRequest request = new CreateSaleRequest("test-idempotency-" + UUID.randomUUID(), List.of(item));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<SaleResponse> response = restTemplate.postForEntity(
                "/sales",
                new HttpEntity<>(request, headers),
                SaleResponse.class);
        
        // 3. Verify
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SaleResponse sale = response.getBody();
        assertThat(sale).isNotNull();
        assertThat(sale.totalAmount()).isEqualByComparingTo("3000.00");
        assertThat(sale.items()).hasSize(1);
        assertThat(sale.items().get(0).lotNumber()).isEqualTo("FLOW-LOT-001");
        
        // 4. Verify in DB
        assertThat(saleRepository.findById(sale.saleId())).isPresent();
    }
}
