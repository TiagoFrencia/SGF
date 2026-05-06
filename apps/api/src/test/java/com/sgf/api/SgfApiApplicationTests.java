package com.sgf.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.modules.catalog.domain.Product;
import com.sgf.modules.catalog.domain.ProductRepository;
import com.sgf.modules.integrations.anmat.domain.AnmatEventStatus;
import com.sgf.modules.integrations.anmat.domain.AnmatEventType;
import com.sgf.modules.integrations.anmat.domain.AnmatTraceabilityEvent;
import com.sgf.modules.integrations.anmat.domain.AnmatTraceabilityEventRepository;
import com.sgf.modules.inventory.domain.Batch;
import com.sgf.modules.inventory.domain.BatchRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SgfApiApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sgf")
            .withUsername("sgf")
            .withPassword("sgf");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    BatchRepository batchRepository;

    @Autowired
    AnmatTraceabilityEventRepository anmatTraceabilityEventRepository;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @DynamicPropertySource
    static void databaseProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void fullFlowWorksAndSalesAreIdempotent() throws Exception {
        String token = login();

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "07791234567890",
                                  "sku": "AMOX-500",
                                  "commercialName": "Amoxicilina 500mg",
                                  "brand": "Genfar",
                                  "activeIngredient": "Amoxicilina",
                                  "prescriptionRequired": true,
                                  "presentationDescription": "Caja x 16 capsulas",
                                  "concentration": "500mg",
                                  "form": "Capsula",
                                  "unitsPerPackage": 16
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("AMOX-500"))
                .andReturn();

        JsonNode productJson = objectMapper.readTree(productResult.getResponse().getContentAsString());
        String productId = productJson.get("id").asText();

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "L-2026-001",
                                  "expiresAt": "%s",
                                  "quantity": 10,
                                  "unitCost": 1400.00
                                }
                                """.formatted(productId, LocalDate.now().plusYears(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(10));

        String salePayload = """
                {
                  "idempotencyKey": "sale-test-001",
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 2,
                      "unitPrice": 2150.00
                    }
                  ]
                }
                """.formatted(productId);

        mockMvc.perform(post("/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(salePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(4300.0))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(post("/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(salePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value("sale-test-001"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(get("/inventory/stock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableQuantity").value(8));

        mockMvc.perform(get("/audit/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").exists());
    }

    @Test
    void saleFailsWithoutEnoughStock() throws Exception {
        String token = login();

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "07791234567891",
                                  "sku": "IBU-400",
                                  "commercialName": "Ibuprofeno 400mg",
                                  "brand": "Genfar",
                                  "activeIngredient": "Ibuprofeno",
                                  "prescriptionRequired": false,
                                  "presentationDescription": "Caja x 10 comprimidos",
                                  "concentration": "400mg",
                                  "form": "Comprimido",
                                  "unitsPerPackage": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "sale-test-002",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 3,
                                      "unitPrice": 999.00
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));
    }

    @Test
    void afipSandboxAuthorizesInvoiceAndIsIdempotentPerSale() throws Exception {
        String token = login();

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "07791234567892",
                                  "sku": "PARA-500",
                                  "commercialName": "Paracetamol 500mg",
                                  "brand": "Genfar",
                                  "activeIngredient": "Paracetamol",
                                  "prescriptionRequired": false,
                                  "presentationDescription": "Caja x 12 comprimidos",
                                  "concentration": "500mg",
                                  "form": "Comprimido",
                                  "unitsPerPackage": 12
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "L-2026-002",
                                  "expiresAt": "%s",
                                  "quantity": 5,
                                  "unitCost": 800.00
                                }
                                """.formatted(productId, LocalDate.now().plusYears(1))))
                .andExpect(status().isOk());

        MvcResult saleResult = mockMvc.perform(post("/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "sale-test-003",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 1,
                                      "unitPrice": 1250.00
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn();

        String saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("saleId").asText();

        String payload = """
                {
                  "invoiceType": "FACTURA_B",
                  "customerDocumentType": "DNI",
                  "customerDocumentNumber": "30111222",
                  "currencyCode": "ARS"
                }
                """;

        MvcResult firstInvoice = mockMvc.perform(post("/afip/invoices/sales/" + saleId + "/authorize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.cae").isNotEmpty())
                .andReturn();

        String invoiceId = objectMapper.readTree(firstInvoice.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/afip/invoices/sales/" + saleId + "/authorize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId))
                .andExpect(jsonPath("$.providerReference").value(org.hamcrest.Matchers.containsString("sandbox")));

        mockMvc.perform(get("/afip/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleId").value(saleId));
    }

    @Test
    void adesfaSandboxValidatesSaleAndPersistsCoverageSplit() throws Exception {
        String token = login();

        mockMvc.perform(get("/adesfa/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").exists())
                .andExpect(jsonPath("$.status").exists());

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "07791234567896",
                                  "sku": "ADESFA-001",
                                  "commercialName": "Producto Cobertura",
                                  "brand": "SGF",
                                  "activeIngredient": "Demo",
                                  "prescriptionRequired": true,
                                  "presentationDescription": "Caja x 20",
                                  "concentration": "500mg",
                                  "form": "Comprimido",
                                  "unitsPerPackage": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "ADESFA-LOT-001",
                                  "expiresAt": "%s",
                                  "quantity": 4,
                                  "unitCost": 1200.00
                                }
                                """.formatted(productId, LocalDate.now().plusYears(1))))
                .andExpect(status().isOk());

        MvcResult saleResult = mockMvc.perform(post("/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "sale-test-adesfa-001",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 2,
                                      "unitPrice": 1500.00
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn();

        String saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("saleId").asText();

        MvcResult validationResult = mockMvc.perform(post("/adesfa/validations/sales/" + saleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionCode": "490120",
                                  "affiliateNumber": "123456789",
                                  "prescriptionNumber": "RX-0001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleId").value(saleId))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.integrationMode").value("SANDBOX"))
                .andExpect(jsonPath("$.coverageAmount").value(2100.00))
                .andExpect(jsonPath("$.patientAmount").value(900.00))
                .andReturn();

        String validationId = objectMapper.readTree(validationResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/adesfa/validations/" + validationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validatorCode").value("PAMI"))
                .andExpect(jsonPath("$.providerReference").exists());
    }

    @Test
    void anmatDispenseRequiresMatchingSale() throws Exception {
        String token = login();

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "07791234567893",
                                  "sku": "TRACE-001",
                                  "commercialName": "Producto Trazable",
                                  "brand": "SGF",
                                  "activeIngredient": "Demo",
                                  "prescriptionRequired": false,
                                  "requiresTraceability": true,
                                  "anmatCategory": "ALTO_RIESGO",
                                  "presentationDescription": "Caja x 1",
                                  "concentration": "1mg",
                                  "form": "Comprimido",
                                  "unitsPerPackage": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "TRACE-LOT-001",
                                  "expiresAt": "%s",
                                  "quantity": 2,
                                  "unitCost": 400.00
                                }
                                """.formatted(productId, LocalDate.now().plusYears(1))))
                .andExpect(status().isOk());

        MvcResult saleResult = mockMvc.perform(post("/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "sale-test-004",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 1,
                                      "unitPrice": 650.00
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn();

        String saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("saleId").asText();
        String dataMatrix = "(01)07791234567893(17)%s(10)TRACE-LOT-001(21)TRACE-SERIAL-001"
                .formatted(LocalDate.now().plusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")));

        mockMvc.perform(post("/anmat/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "DISPENSE",
                                  "dataMatrix": "%s",
                                  "gln": "7791234500001",
                                  "saleId": "%s",
                                  "source": "SCANNER",
                                  "occurredAt": "2026-05-04T22:50:00Z"
                                }
                                """.formatted(dataMatrix, saleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("DISPENSE"))
                .andExpect(jsonPath("$.saleId").value(saleId));

        mockMvc.perform(post("/anmat/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "DISPENSE",
                                  "dataMatrix": "%s",
                                  "gln": "7791234500001",
                                  "source": "SCANNER",
                                  "occurredAt": "2026-05-04T22:51:00Z"
                                }
                                """.formatted("(01)07791234567893(17)%s(10)TRACE-LOT-001(21)TRACE-SERIAL-002"
                                        .formatted(LocalDate.now().plusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("DISPENSE event requires a saleId"));
    }

    @Test
    void anmatDispenseRequiresPriorReceiptAndSummaryWorks() throws Exception {
        String token = login();

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "07791234567894",
                                  "sku": "TRACE-002",
                                  "commercialName": "Producto Trazable 2",
                                  "brand": "SGF",
                                  "activeIngredient": "Demo",
                                  "prescriptionRequired": false,
                                  "requiresTraceability": true,
                                  "anmatCategory": "ALTO_RIESGO",
                                  "presentationDescription": "Caja x 1",
                                  "concentration": "1mg",
                                  "form": "Comprimido",
                                  "unitsPerPackage": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "TRACE-LOT-002",
                                  "expiresAt": "%s",
                                  "quantity": 2,
                                  "unitCost": 400.00
                                }
                                """.formatted(productId, LocalDate.now().plusYears(1))))
                .andExpect(status().isOk());

        MvcResult saleResult = mockMvc.perform(post("/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "sale-test-005",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 1,
                                      "unitPrice": 650.00
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn();

        String saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("saleId").asText();
        String expiry = LocalDate.now().plusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        String serialWithoutReceipt = "(01)07791234567894(17)%s(10)TRACE-LOT-002(21)TRACE-SERIAL-010".formatted(expiry);

        mockMvc.perform(post("/anmat/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "DISPENSE",
                                  "dataMatrix": "%s",
                                  "gln": "7791234500001",
                                  "saleId": "%s",
                                  "source": "SCANNER",
                                  "occurredAt": "2026-05-04T22:55:00Z"
                                }
                                """.formatted(serialWithoutReceipt, saleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("DISPENSE event requires a prior RECEIPT for the same serial"));

        String serialWithReceipt = "(01)07791234567894(17)%s(10)TRACE-LOT-002(21)TRACE-SERIAL-011".formatted(expiry);

        mockMvc.perform(post("/anmat/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "RECEIPT",
                                  "dataMatrix": "%s",
                                  "gln": "7791234500001",
                                  "source": "SCANNER",
                                  "occurredAt": "2026-05-04T22:56:00Z"
                                }
                                """.formatted(serialWithReceipt)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/anmat/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "DISPENSE",
                                  "dataMatrix": "%s",
                                  "gln": "7791234500001",
                                  "saleId": "%s",
                                  "source": "SCANNER",
                                  "occurredAt": "2026-05-04T22:57:00Z"
                                }
                                """.formatted(serialWithReceipt, saleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("DISPENSE"))
                .andExpect(jsonPath("$.integrationMode").exists())
                .andExpect(jsonPath("$.retryable").exists());

        mockMvc.perform(get("/anmat/serial-summary")
                        .header("Authorization", "Bearer " + token)
                        .param("gtin", "07791234567894")
                        .param("serialNumber", "TRACE-SERIAL-011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReceipt").value(true))
                .andExpect(jsonPath("$.hasDispense").value(true))
                .andExpect(jsonPath("$.currentState").value("DISPENSE"));
    }

    @Test
    void anmatDashboardAndInconsistenciesEndpointsRespond() throws Exception {
        String token = login();

        mockMvc.perform(get("/anmat/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").exists())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(get("/anmat/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").exists())
                .andExpect(jsonPath("$.inconsistentSerials").exists());

        mockMvc.perform(get("/anmat/inconsistencies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/anmat/remediation-cases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void anmatRemediationWorkflowCreatesAndUpdatesCases() throws Exception {
        String token = login();

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "07791234567895",
                                  "sku": "TRACE-003",
                                  "commercialName": "Producto Trazable 3",
                                  "brand": "SGF",
                                  "activeIngredient": "Demo",
                                  "prescriptionRequired": false,
                                  "requiresTraceability": true,
                                  "anmatCategory": "ALTO_RIESGO",
                                  "presentationDescription": "Caja x 1",
                                  "concentration": "1mg",
                                  "form": "Comprimido",
                                  "unitsPerPackage": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "TRACE-LOT-003",
                                  "expiresAt": "%s",
                                  "quantity": 1,
                                  "unitCost": 400.00
                                }
                                """.formatted(productId, LocalDate.now().plusYears(1))))
                .andExpect(status().isOk());

        Product product = productRepository.findById(java.util.UUID.fromString(productId)).orElseThrow();
        Batch batch = batchRepository.findByProductIdAndLotNumber(product.getId(), "TRACE-LOT-003").orElseThrow();

        AnmatTraceabilityEvent inconsistentEvent = new AnmatTraceabilityEvent();
        inconsistentEvent.setProduct(product);
        inconsistentEvent.setBatch(batch);
        inconsistentEvent.setEventType(AnmatEventType.DISPENSE);
        inconsistentEvent.setEventStatus(AnmatEventStatus.REPORTED);
        inconsistentEvent.setGtin("07791234567895");
        inconsistentEvent.setSerialNumber("TRACE-SERIAL-999");
        inconsistentEvent.setLotNumber("TRACE-LOT-003");
        inconsistentEvent.setExpiresAt(LocalDate.now().plusYears(1));
        inconsistentEvent.setGln("7791234500001");
        inconsistentEvent.setOccurredAt(OffsetDateTime.parse("2026-05-04T23:00:00Z"));
        inconsistentEvent.setSource("LEGACY_IMPORT");
        inconsistentEvent.setRequestJson("{\"source\":\"legacy\"}");
        inconsistentEvent.setResponseJson("{\"status\":\"reported\"}");
        anmatTraceabilityEventRepository.save(inconsistentEvent);

        mockMvc.perform(post("/anmat/remediation-cases/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inconsistenciesFound").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.remediationCasesCreated").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult casesResult = mockMvc.perform(get("/anmat/remediation-cases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].issueCode").exists())
                .andExpect(jsonPath("$.page").value(0))
                .andReturn();

        String caseId = objectMapper.readTree(casesResult.getResponse().getContentAsString()).get("items").get(0).get("id").asText();

        mockMvc.perform(patch("/anmat/remediation-cases/" + caseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACKNOWLEDGED",
                                  "notes": "Revisado por operaciones",
                                  "assignedTo": "farmacia-central",
                                  "reason": "Pendiente de validacion manual"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.assignedTo").value("farmacia-central"));

        mockMvc.perform(get("/anmat/remediation-cases")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "ACKNOWLEDGED")
                        .param("assignedTo", "farmacia-central")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "status")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.items[0].assignedTo").value("farmacia-central"))
                .andExpect(jsonPath("$.sortBy").value("status"))
                .andExpect(jsonPath("$.sortDirection").value("ASC"));

        mockMvc.perform(patch("/anmat/remediation-cases/" + caseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "reason": "Serial conciliado contra remito fisico"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.lastReason").value("Serial conciliado contra remito fisico"))
                .andExpect(jsonPath("$.resolvedAt").exists());

        mockMvc.perform(patch("/anmat/remediation-cases/" + caseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OPEN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reopening a remediation case requires a reason"));

        mockMvc.perform(patch("/anmat/remediation-cases/" + caseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OPEN",
                                  "reason": "Nueva evidencia operativa requiere reanalisis"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.lastReason").value("Nueva evidencia operativa requiere reanalisis"))
                .andExpect(jsonPath("$.resolvedAt").value(org.hamcrest.Matchers.nullValue()));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
