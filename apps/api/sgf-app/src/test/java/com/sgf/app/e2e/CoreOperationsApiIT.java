package com.sgf.app.e2e;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgf.app.support.PostgresIntegrationTestSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class CoreOperationsApiIT extends PostgresIntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void catalogInventoryContractsAndAlertsWork() throws Exception {
        String token = login();
        String gtin = "07791234567910";
        String sku = "CORE-INV-001";
        String expiry = LocalDate.now().plusDays(20).toString();

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "%s",
                                  "sku": "%s",
                                  "commercialName": "Producto Core Inventario",
                                  "brand": "SGF",
                                  "activeIngredient": "Demo",
                                  "prescriptionRequired": false,
                                  "presentationDescription": "Caja x 1",
                                  "concentration": "500mg",
                                  "form": "Comprimido",
                                  "unitsPerPackage": 1
                                }
                                """.formatted(gtin, sku)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku))
                .andReturn();

        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku").value(hasItem(sku)));

        mockMvc.perform(get("/products/" + productId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtin").value(gtin));

        mockMvc.perform(get("/products/search/gtin/" + gtin)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId));

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "CORE-LOT-001",
                                  "expiresAt": "%s",
                                  "quantity": 1,
                                  "unitCost": 500.00
                                }
                                """.formatted(productId, expiry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(1));

        mockMvc.perform(get("/inventory/products/" + productId + "/batches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lotNumber").value("CORE-LOT-001"));

        mockMvc.perform(get("/inventory/stock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == '%s')].availableQuantity".formatted(sku)).value(hasItem(1)));

        mockMvc.perform(get("/inventory/alerts/expiry")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.lotNumber == 'CORE-LOT-001')]").isNotEmpty());

        mockMvc.perform(post("/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "core-alert-001",
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 1,
                                      "unitPrice": 900.00
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/inventory/alerts/reorder")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.gtin == '%s')].needsReorder".formatted(gtin)).value(hasItem(true)));
    }

    @Test
    void posOrdersTransferLifecycleAndAuditWork() throws Exception {
        String token = login();
        String gtin = "07791234567911";
        String sku = "CORE-POS-001";
        String expiry = LocalDate.now().plusYears(1).toString();

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "%s",
                                  "sku": "%s",
                                  "commercialName": "Producto Core POS",
                                  "brand": "SGF",
                                  "activeIngredient": "Demo",
                                  "prescriptionRequired": false,
                                  "presentationDescription": "Caja x 12",
                                  "concentration": "500mg",
                                  "form": "Comprimido",
                                  "unitsPerPackage": 12
                                }
                                """.formatted(gtin, sku)))
                .andExpect(status().isOk())
                .andReturn();

        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "CORE-LOT-002",
                                  "expiresAt": "%s",
                                  "quantity": 5,
                                  "unitCost": 600.00
                                }
                                """.formatted(productId, expiry)))
                .andExpect(status().isOk());

        MvcResult batches = mockMvc.perform(get("/inventory/products/" + productId + "/batches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String batchId = objectMapper.readTree(batches.getResponse().getContentAsString()).get(0).get("batchId").asText();

        String branchId = "00000000-0000-0000-0000-000000000101";
        String otherBranchId = "00000000-0000-0000-0000-000000000202";

        MvcResult draftResult = mockMvc.perform(post("/pos/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "customerName": "Cliente Demo",
                                  "customerDocument": "30111222",
                                  "notes": "Core API"
                                }
                                """.formatted(branchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        String orderId = objectMapper.readTree(draftResult.getResponse().getContentAsString()).get("orderId").asText();

        mockMvc.perform(post("/pos/orders/" + orderId + "/scan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "%s",
                                  "quantity": 1,
                                  "unitPrice": 1200.00
                                }
                                """.formatted(gtin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(1));

        mockMvc.perform(post("/pos/orders/" + orderId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "idempotencyKey": "pos-before-ready"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("READY")));

        mockMvc.perform(patch("/pos/orders/" + orderId + "/ready")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        mockMvc.perform(post("/pos/orders/" + orderId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "idempotencyKey": "pos-complete-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.saleId").exists());

        mockMvc.perform(get("/pos/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        MvcResult transferResult = mockMvc.perform(post("/inventory/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceBranchId": "%s",
                                  "destinationBranchId": "%s",
                                  "productId": "%s",
                                  "batchId": "%s",
                                  "quantity": 2,
                                  "notes": "Reposición"
                                }
                                """.formatted(branchId, otherBranchId, productId, batchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        JsonNode transferJson = objectMapper.readTree(transferResult.getResponse().getContentAsString());
        String transferId = transferJson.get("id").asText();

        mockMvc.perform(patch("/inventory/transfers/" + transferId + "/ship")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s"
                                }
                                """.formatted(branchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));

        mockMvc.perform(patch("/inventory/transfers/" + transferId + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s"
                                }
                                """.formatted(branchId)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/inventory/transfers/" + transferId + "/receive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "receivedQuantity": 2
                                }
                                """.formatted(otherBranchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        mockMvc.perform(get("/inventory/transfers")
                        .header("Authorization", "Bearer " + token)
                        .param("sourceBranchId", branchId)
                        .param("status", "RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(transferId)));

        mockMvc.perform(get("/audit/events")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("BRANCH_TRANSFER_RECEIVED")));
    }

    @Test
    void multiTerminalTransferDisputeAndAuditVerificationWork() throws Exception {
        String token = login();
        String gtin = "07791234567912";
        String sku = "CORE-MULTI-001";
        String expiry = LocalDate.now().plusYears(1).format(DateTimeFormatter.ISO_DATE);
        String branchId = "00000000-0000-0000-0000-000000000101";
        String otherBranchId = "00000000-0000-0000-0000-000000000202";
        String terminalId = "POS-TERM-ALFA";

        MvcResult productResult = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gtin": "%s",
                                  "sku": "%s",
                                  "commercialName": "Producto Multi Terminal",
                                  "brand": "SGF",
                                  "activeIngredient": "Demo",
                                  "prescriptionRequired": false,
                                  "presentationDescription": "Caja x 4",
                                  "concentration": "200mg",
                                  "form": "Capsula",
                                  "unitsPerPackage": 4
                                }
                                """.formatted(gtin, sku)))
                .andExpect(status().isOk())
                .andReturn();
        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/inventory/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "lotNumber": "CORE-LOT-003",
                                  "expiresAt": "%s",
                                  "quantity": 4,
                                  "unitCost": 450.00
                                }
                                """.formatted(productId, expiry)))
                .andExpect(status().isOk());

        MvcResult batches = mockMvc.perform(get("/inventory/products/" + productId + "/batches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String batchId = objectMapper.readTree(batches.getResponse().getContentAsString()).get(0).get("batchId").asText();

        MvcResult firstOrder = mockMvc.perform(post("/pos/orders/terminals/" + terminalId + "/new")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "customerName": "Cliente Uno",
                                  "customerDocument": "30111000",
                                  "notes": "Terminal alfa"
                                }
                                """.formatted(branchId)))
                .andExpect(status().isOk())
                .andReturn();
        String firstOrderId = objectMapper.readTree(firstOrder.getResponse().getContentAsString()).get("orderId").asText();

        MvcResult secondOrder = mockMvc.perform(post("/pos/orders/terminals/" + terminalId + "/new")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "customerName": "Cliente Dos",
                                  "customerDocument": "30111001",
                                  "notes": "Terminal alfa"
                                }
                                """.formatted(branchId)))
                .andExpect(status().isOk())
                .andReturn();
        String secondOrderId = objectMapper.readTree(secondOrder.getResponse().getContentAsString()).get("orderId").asText();

        mockMvc.perform(get("/pos/orders/terminals/" + terminalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].orderId").value(hasItem(firstOrderId)))
                .andExpect(jsonPath("$[*].orderId").value(hasItem(secondOrderId)));

        mockMvc.perform(get("/pos/orders/terminals/" + terminalId + "/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(secondOrderId));

        mockMvc.perform(patch("/pos/orders/terminals/" + terminalId + "/switch/" + firstOrderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(firstOrderId));

        mockMvc.perform(patch("/pos/orders/terminals/" + terminalId + "/switch/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/pos/orders/terminals/" + terminalId + "/orders/" + secondOrderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/pos/orders/terminals/" + terminalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/pos/orders/terminals/" + terminalId + "/recover")
                        .header("Authorization", "Bearer " + token)
                        .param("branchId", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveredOrders").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeOrderId").exists());

        MvcResult transferResult = mockMvc.perform(post("/inventory/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceBranchId": "%s",
                                  "destinationBranchId": "%s",
                                  "productId": "%s",
                                  "batchId": "%s",
                                  "quantity": 2,
                                  "notes": "Transferencia disputada"
                                }
                                """.formatted(branchId, otherBranchId, productId, batchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String transferId = objectMapper.readTree(transferResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/inventory/transfers/" + transferId + "/ship")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s"
                                }
                                """.formatted(otherBranchId)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/inventory/transfers/" + transferId + "/ship")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s"
                                }
                                """.formatted(branchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));

        mockMvc.perform(patch("/inventory/transfers/" + transferId + "/receive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "receivedQuantity": 1
                                }
                                """.formatted(branchId)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/inventory/transfers/" + transferId + "/receive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "receivedQuantity": 1
                                }
                                """.formatted(otherBranchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPUTED"))
                .andExpect(jsonPath("$.receivedQuantity").value(1));

        mockMvc.perform(get("/inventory/transfers")
                        .header("Authorization", "Bearer " + token)
                        .param("destinationBranchId", otherBranchId)
                        .param("status", "DISPUTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(transferId)));

        mockMvc.perform(get("/audit/events")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("BRANCH_TRANSFER_CREATED")))
                .andExpect(jsonPath("$[*].eventType").value(hasItem("BRANCH_TRANSFER_SHIPPED")))
                .andExpect(jsonPath("$[*].eventType").value(hasItem("BRANCH_TRANSFER_DISPUTED")));

        mockMvc.perform(get("/audit/events/verify")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.verifiedEvents").value(greaterThanOrEqualTo(1)));
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
