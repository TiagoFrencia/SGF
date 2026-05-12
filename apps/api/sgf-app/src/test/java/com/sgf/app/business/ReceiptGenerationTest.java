package com.sgf.app.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.sgf.catalog.domain.Product;
import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleItem;
import com.sgf.pos.service.ReceiptService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ReceiptGenerationTest {

    private final ReceiptService receiptService = new ReceiptService();

    @Test
    void shouldGenerateTicketWithCorrectData() {
        // Given
        Sale sale = new Sale();
        sale.setSoldAt(OffsetDateTime.now());
        sale.setCreatedBy("tiago_pos");
        sale.setPaymentMethod("CASH");
        sale.setTotalAmount(new BigDecimal("3500.00"));
        sale.setItems(new ArrayList<>());

        Product p1 = new Product();
        p1.setCommercialName("IBUPROFENO 600");
        
        SaleItem item = new SaleItem();
        item.setProduct(p1);
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("3000.00"));
        sale.getItems().add(item);

        // When
        String ticket = receiptService.generatePlainTicket(sale);

        // Then
        assertThat(ticket).contains("SGF - FARMACIA");
        assertThat(ticket).contains("Vendedor: tiago_pos");
        assertThat(ticket).contains("TOTAL:");
        assertThat(ticket).contains("$3500");
        assertThat(ticket).contains("IBUPROFENO 600");
    }
}
