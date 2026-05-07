package com.sgf.inventory.web;

import com.sgf.inventory.service.ExpiryAlertService;
import com.sgf.inventory.service.ExpiryAlertService.ExpiryAlert;
import com.sgf.inventory.service.ReorderPointService;
import com.sgf.inventory.service.ReorderPointService.ReorderCalculation;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory/alerts")
public class InventoryAlertController {

    private final ExpiryAlertService expiryAlertService;
    private final ReorderPointService reorderPointService;

    public InventoryAlertController(ExpiryAlertService expiryAlertService,
                                    ReorderPointService reorderPointService) {
        this.expiryAlertService = expiryAlertService;
        this.reorderPointService = reorderPointService;
    }

    /**
     * GET /inventory/alerts/expiry?days=30
     * Returns batches expiring within the given window.
     */
    @GetMapping("/expiry")
    public List<ExpiryAlert> expiryAlerts(@RequestParam(defaultValue = "90") int days) {
        return expiryAlertService.getExpiryAlerts(days);
    }

    /**
     * GET /inventory/alerts/reorder
     * Returns products that need reorder based on dynamic reorder point.
     */
    @GetMapping("/reorder")
    public List<ReorderCalculation> reorderAlerts() {
        return reorderPointService.listReorderAlerts(90, 7);
    }

    /**
     * GET /inventory/alerts/reorder/{productId}
     * Calculates reorder point for a specific product on demand.
     */
    @GetMapping("/reorder/{productId}")
    public ReorderCalculation productReorderPoint(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "90") int analysisDays,
            @RequestParam(defaultValue = "7") int leadTimeDays) {
        return reorderPointService.calculate(productId, analysisDays, leadTimeDays);
    }
}