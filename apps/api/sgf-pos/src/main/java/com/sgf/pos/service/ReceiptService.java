package com.sgf.pos.service;

import com.sgf.pos.domain.Sale;
import com.sgf.pos.domain.SaleItem;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class ReceiptService {

    public String generatePlainTicket(Sale sale) {
        StringBuilder ticket = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        ticket.append("==============================\n");
        ticket.append("       SGF - FARMACIA         \n");
        ticket.append("==============================\n");
        ticket.append("Fecha: ").append(sale.getSoldAt().format(formatter)).append("\n");
        ticket.append("Vendedor: ").append(sale.getCreatedBy()).append("\n");
        ticket.append("Pago: ").append(sale.getPaymentMethod()).append("\n");
        ticket.append("------------------------------\n");

        for (SaleItem item : sale.getItems()) {
            ticket.append(String.format("%-15s x%d  $%7.2f\n",
                    truncate(item.getProduct().getCommercialName(), 15),
                    item.getQuantity(),
                    item.getSubtotal()));
        }

        ticket.append("------------------------------\n");
        ticket.append(String.format("TOTAL:              $%7.2f\n", sale.getTotalAmount()));
        ticket.append("==============================\n");
        ticket.append("   GRACIAS POR SU COMPRA      \n");
        ticket.append("==============================\n");

        return ticket.toString();
    }

    private String truncate(String text, int length) {
        if (text.length() <= length) return text;
        return text.substring(0, length - 2) + "..";
    }
}
